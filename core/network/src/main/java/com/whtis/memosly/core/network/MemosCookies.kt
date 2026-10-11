package com.whtis.memosly.core.network

import okhttp3.Headers

/**
 * Helpers for the memos auth cookies.
 *
 * The gRPC-gateway surfaces cookies the server sets as `Grpc-Metadata-Set-Cookie`
 * (the response we saw from sign-in), and only accepts them back via the
 * `Grpc-Metadata-Cookie` request header — a plain `Cookie` header never reaches
 * the gRPC metadata and the server treats the request as unauthenticated.
 */
object MemosCookies {
    const val REFRESH_COOKIE_NAME = "memos_refresh"

    /** Reads a cookie value out of `Grpc-Metadata-Set-Cookie` or `Set-Cookie`. */
    fun fromResponseHeaders(headers: Headers, cookieName: String = REFRESH_COOKIE_NAME): String? {
        for (headerName in listOf("grpc-metadata-set-cookie", "set-cookie")) {
            val cookieHeader = headers[headerName] ?: continue
            val token = cookieHeader
                .split(";")
                .firstOrNull { it.trim().startsWith("$cookieName=") }
                ?.substringAfter("$cookieName=")
                ?.trim()
            if (!token.isNullOrEmpty()) return token
        }
        return null
    }

    /** The value to send in the `Grpc-Metadata-Cookie` request header. */
    fun asRequestHeader(cookieValue: String): String = "$REFRESH_COOKIE_NAME=$cookieValue"
}
