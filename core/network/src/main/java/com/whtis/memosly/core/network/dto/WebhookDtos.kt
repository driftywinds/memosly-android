package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebhookDto(
    val id: Int,
    val creatorId: Int,
    val name: String,
    val url: String,
    val createTime: String,
    val updateTime: String,
)

@JsonClass(generateAdapter = true)
data class CreateWebhookRequest(
    val name: String,
    val url: String,
)

@JsonClass(generateAdapter = true)
data class ListWebhooksResponse(
    val webhooks: List<WebhookDto>,
)

// --- v0.31 user webhooks ---
// Same path as v0.25's nested listing, but the payload changed: no numeric id or
// creatorId, and the title moved to `displayName`. Parsing the old DTO against it
// throws (required fields missing), which is why this gets its own type.

@JsonClass(generateAdapter = true)
data class WebhookDtoV031(
    val name: String = "",
    val url: String = "",
    val displayName: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)

@JsonClass(generateAdapter = true)
data class ListUserWebhooksResponseV031(
    val webhooks: List<WebhookDtoV031> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreateWebhookV031Request(
    val url: String,
    val displayName: String? = null,
)
