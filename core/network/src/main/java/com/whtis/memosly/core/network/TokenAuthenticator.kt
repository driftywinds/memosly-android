package com.whtis.memosly.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps v0.31 sessions alive past the 15-minute access-token lifetime.
 *
 * When the server answers 401 to a request from the memos host, this exchanges the
 * stored `memos_refresh` cookie for a fresh access token and replays the request once.
 * Concurrent 401s are collapsed: the first caller rotates the cookie, the rest see that
 * the stored token no longer matches the one the failed request used and just retry
 * with it. Without a refresh cookie (access-token/PAT logins) nothing happens — the
 * 401 propagates like before.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (tokenManager.serverVersion.value != ServerVersion.V031) return null
        // Two attempts are enough: original → refreshed → give up. Without this an
        // invalid refresh cookie would loop forever.
        if (responseCount(response) >= 2) return null
        if (tokenManager.refreshCookie.value == null) return null
        // Only the memos server can mint tokens; never "refresh" toward an S3 bucket
        // that happened to answer 401 to a presigned request.
        if (!isMemosServerHost(response.request.url)) return null

        synchronized(this) {
            val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = tokenManager.accessToken.value
            if (!currentToken.isNullOrBlank() && currentToken != usedToken) {
                // Another call already refreshed while we waited on the lock.
                return response.request.withBearer(currentToken)
            }
            val refreshed = runBlocking { tokenRefresher.refresh() }
            val newToken = tokenManager.accessToken.value
            if (newToken.isNullOrBlank() || (!refreshed && newToken == usedToken)) {
                return null
            }
            return response.request.withBearer(newToken)
        }
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private fun isMemosServerHost(url: HttpUrl): Boolean {
        if (url.host == PLACEHOLDER_HOST) return true
        val configured = tokenManager.serverUrl.value ?: return false
        val configuredHost = configured.toHttpUrlOrNull()?.host
            ?: "https://$configured".toHttpUrlOrNull()?.host
            ?: return false
        return url.host.equals(configuredHost, ignoreCase = true)
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
