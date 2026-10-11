package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InboxMessageDto(
    val name: String,
    val sender: String,
    val receiver: String,
    val status: String,
    val createTime: String,
    val type: String?,
    val activityId: Int?,
)

@JsonClass(generateAdapter = true)
data class ListInboxResponse(
    val inboxes: List<InboxMessageDto>,
)

@JsonClass(generateAdapter = true)
data class UpdateInboxRequest(
    val status: String,
)

// --- v0.31 user notifications ---
// The inboxes service is gone; notifications live under the user resource.

@JsonClass(generateAdapter = true)
data class UserNotificationDtoV031(
    val name: String = "",
    val sender: String? = null,
    val status: String? = null,
    val createTime: String? = null,
    val type: String? = null,
)

@JsonClass(generateAdapter = true)
data class ListUserNotificationsResponseV031(
    val notifications: List<UserNotificationDtoV031> = emptyList(),
)
