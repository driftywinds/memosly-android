package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.model.UserAccessToken
import com.whtis.memosly.core.model.UserStats
import com.whtis.memosly.core.model.Webhook

interface UserRepository {
    /**
     * User-scoped calls take the full resource name (`users/{user}`). The trailing
     * segment is the path component on every supported server: numeric ids up to
     * v0.26, usernames from v0.31 on — passing an id would 404 against v0.31.
     */
    suspend fun getUser(name: String): User
    suspend fun updateUser(name: String, nickname: String?, email: String?, avatarUrl: String?, description: String?): User
    suspend fun getUserStats(name: String): UserStats
    suspend fun listAccessTokens(name: String): List<UserAccessToken>
    suspend fun createAccessToken(name: String, description: String, expiresAt: String?): Unit
    suspend fun deleteAccessToken(name: String, token: String)
    suspend fun listWebhooks(name: String): List<Webhook>
    suspend fun createWebhook(owner: String, title: String, url: String): Webhook
    suspend fun deleteWebhook(webhook: Webhook)
}
