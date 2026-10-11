package com.whtis.memosly.core.network

import com.squareup.moshi.Moshi
import com.whtis.memosly.core.network.dto.RefreshTokenResponseV031
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Exchanges the `memos_refresh` cookie for a fresh access token (v0.31).
 *
 * v0.31 access tokens live 15 minutes; the refresh token is a 30-day cookie that the
 * server rotates on every use, so the stored cookie must be replaced after each call.
 * Refreshes are serialized — a second concurrent refresh with the already-rotated
 * cookie is rejected by the server with 401.
 *
 * Uses its own plain [OkHttpClient]: this class is reached from the main client's
 * Authenticator, and routing the refresh through that same client would recurse.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionPreferences: SessionPreferences,
    private val moshi: Moshi,
    @Named("plain") private val client: OkHttpClient,
) {
    private val mutex = Mutex()

    /** Returns true once a new access token has been stored (locally and persistently). */
    suspend fun refresh(): Boolean = mutex.withLock {
        val cookieValue = tokenManager.refreshCookie.value ?: return@withLock false
        val serverUrl = tokenManager.serverUrl.value ?: return@withLock false

        val request = Request.Builder()
            .url("$serverUrl/api/v1/auth/refresh")
            .addHeader("Grpc-Metadata-Cookie", MemosCookies.asRequestHeader(cookieValue))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val body = response.body?.string().orEmpty()
                    val parsed = moshi.adapter(RefreshTokenResponseV031::class.java)
                        .lenient()
                        .fromJson(body)
                        ?: return@withContext false
                    val newToken = parsed.accessToken?.takeIf { it.isNotBlank() }
                        ?: return@withContext false

                    tokenManager.setAccessToken(newToken)
                    sessionPreferences.accessToken = newToken

                    // The server rotates the refresh cookie with every exchange.
                    MemosCookies.fromResponseHeaders(response.headers)?.let { rotated ->
                        tokenManager.setRefreshCookie(rotated)
                        sessionPreferences.refreshCookie = rotated
                    }
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
