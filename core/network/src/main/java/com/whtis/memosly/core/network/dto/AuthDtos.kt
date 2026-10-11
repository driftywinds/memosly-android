package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PasswordCredentials(
    val username: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class SignInRequestBody(
    val passwordCredentials: PasswordCredentials,
    val neverExpire: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SignInResponseV026(
    val user: UserDto? = null,
    val accessToken: String? = null,
    val accessTokenExpiresAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SessionResponse(
    val user: UserDto? = null,
    val lastAccessedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class GetCurrentUserResponse(
    val user: UserDto? = null,
)

/** v0.31: POST /api/v1/auth/refresh — empty body; the refresh token travels in a cookie. */
@JsonClass(generateAdapter = true)
class RefreshTokenV031Request

@JsonClass(generateAdapter = true)
data class RefreshTokenResponseV031(
    val accessToken: String? = null,
    val expiresAt: String? = null,
)
