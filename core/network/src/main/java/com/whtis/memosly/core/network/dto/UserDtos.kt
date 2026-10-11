package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    val name: String,
    val role: String,
    val username: String,
    val email: String = "",
    val nickname: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val description: String = "",
    val createTime: String = "",
    val updateTime: String = "",
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val nickname: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val password: String? = null,
)

@JsonClass(generateAdapter = true)
data class UserStatsDto(
    val memoDisplayTimestamps: List<String>? = null,
    val memoTypeStats: Map<String, Int>? = null,
    val tagCount: Map<String, Int>? = null,
    // v0.31: display timestamps are gone; created timestamps + an explicit count replace them.
    val memoCreatedTimestamps: List<String>? = null,
    val totalMemoCount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class UserSettingDto(
    val locale: String?,
    val appearance: String?,
    val memoVisibility: String?,
)

@JsonClass(generateAdapter = true)
data class UpdateUserSettingRequest(
    val locale: String? = null,
    val appearance: String? = null,
    val memoVisibility: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateAccessTokenRequest(
    val description: String,
    val expiresAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ListAccessTokensResponse(
    val accessTokens: List<AccessTokenDto>,
)

@JsonClass(generateAdapter = true)
data class AccessTokenDto(
    val accessToken: String,
    val description: String,
    val issuedAt: String,
    val expiresAt: String?,
)

// --- v0.31 personal access tokens ---
// The list response never contains the token secret (it is shown once at creation),
// and the resource name carries a UUID id used for deletion.

@JsonClass(generateAdapter = true)
data class PersonalAccessTokenDtoV031(
    val name: String = "",
    val description: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val lastUsedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ListPersonalAccessTokensResponseV031(
    val personalAccessTokens: List<PersonalAccessTokenDtoV031> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreateAccessTokenV031Request(
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateAccessTokenV031Response(
    val personalAccessToken: PersonalAccessTokenDtoV031? = null,
    val token: String? = null,
)
