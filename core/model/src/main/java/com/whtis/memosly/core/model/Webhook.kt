package com.whtis.memosly.core.model

data class Webhook(
    /** Server-side id — the trailing segment of the webhook resource name. */
    val id: String,
    val creatorId: Int,
    /** The webhook title (v0.31 returns it as `displayName`). */
    val name: String,
    val url: String,
    val createTime: String,
    val updateTime: String,
    /** Full resource name (`users/{user}/webhooks/{webhook}`); v0.31 deletes by it. */
    val resourceName: String = "",
)
