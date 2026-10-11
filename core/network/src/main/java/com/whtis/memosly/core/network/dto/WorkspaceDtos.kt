package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkspaceProfileDto(
    val owner: String,
    val version: String,
    val mode: String,
)

// --- v0.31 instance profile ---
// Replaces /api/v1/workspace/profile, which no longer exists. No owner or mode —
// the "admin" user and the demo/access flags stand in for them.

@JsonClass(generateAdapter = true)
data class InstanceProfileDtoV031(
    val version: String = "",
    val demo: Boolean = false,
    val instanceUrl: String? = null,
    val admin: UserDto? = null,
    val commit: String? = null,
    val needsSetup: Boolean? = null,
    val accessMode: String? = null,
)

@JsonClass(generateAdapter = true)
data class WorkspaceSettingDto(
    val generalSetting: WorkspaceGeneralSettingDto?,
)

@JsonClass(generateAdapter = true)
data class WorkspaceGeneralSettingDto(
    val additionalScript: String?,
    val additionalStyle: String?,
    val customProfile: CustomProfileDto?,
)

@JsonClass(generateAdapter = true)
data class CustomProfileDto(
    val title: String?,
    val description: String?,
    val logoUrl: String?,
    val locale: String?,
    val appearance: String?,
)
