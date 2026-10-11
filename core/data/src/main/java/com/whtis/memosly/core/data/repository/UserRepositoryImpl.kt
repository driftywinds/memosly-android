package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.model.UserAccessToken
import com.whtis.memosly.core.model.UserStats
import com.whtis.memosly.core.model.Webhook
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.UserApi
import com.whtis.memosly.core.network.api.WebhookApi
import com.whtis.memosly.core.network.dto.CreateAccessTokenRequest
import com.whtis.memosly.core.network.dto.CreateAccessTokenV031Request
import com.whtis.memosly.core.network.dto.CreateWebhookRequest
import com.whtis.memosly.core.network.dto.CreateWebhookV031Request
import com.whtis.memosly.core.network.dto.UpdateUserRequest
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val webhookApi: WebhookApi,
    private val tokenManager: TokenManager,
) : UserRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    /** Path segment of a `users/{user}` resource name — id on old servers, username on v0.31. */
    private fun userSegment(name: String) = name.substringAfterLast("/")

    override suspend fun getUser(name: String): User =
        userApi.getUser(userSegment(name)).toDomain()

    override suspend fun updateUser(
        name: String,
        nickname: String?,
        email: String?,
        avatarUrl: String?,
        description: String?,
    ): User = userApi.updateUser(
        userSegment(name),
        UpdateUserRequest(
            nickname = nickname,
            email = email,
            avatarUrl = avatarUrl,
            description = description,
        ),
    ).toDomain()

    override suspend fun getUserStats(name: String): UserStats =
        when (version) {
            ServerVersion.V024 -> userApi.getUserStats(userSegment(name))
            // v0.25 + v0.26 + v0.31 all use :getStats
            else -> userApi.getUserStatsV026(userSegment(name))
        }.toDomain()

    override suspend fun listAccessTokens(name: String): List<UserAccessToken> {
        val segment = userSegment(name)
        return when (version) {
            ServerVersion.V024 -> userApi.listAccessTokens(segment).accessTokens.map { it.toDomain() }
            ServerVersion.V025 -> userApi.listAccessTokensV025(segment).accessTokens.map { it.toDomain() }
            ServerVersion.V026 -> userApi.listAccessTokensV026(segment).accessTokens.map { it.toDomain() }
            ServerVersion.V031 ->
                userApi.listAccessTokensV031(segment).personalAccessTokens.map { it.toDomain() }
        }
    }

    override suspend fun createAccessToken(name: String, description: String, expiresAt: String?) {
        val segment = userSegment(name)
        when (version) {
            ServerVersion.V024 ->
                userApi.createAccessToken(segment, CreateAccessTokenRequest(description, expiresAt))
            ServerVersion.V025 ->
                userApi.createAccessTokenV025(segment, CreateAccessTokenRequest(description, expiresAt))
            ServerVersion.V026 ->
                userApi.createAccessTokenV026(segment, CreateAccessTokenRequest(description, expiresAt))
            // v0.31 takes a lifetime in days, not an absolute expiry; null means no expiry.
            ServerVersion.V031 ->
                userApi.createAccessTokenV031(segment, CreateAccessTokenV031Request(description))
        }
    }

    override suspend fun deleteAccessToken(name: String, token: String) {
        val segment = userSegment(name)
        when (version) {
            ServerVersion.V024 -> userApi.deleteAccessToken(segment, token)
            ServerVersion.V025 -> userApi.deleteAccessTokenV025(segment, token)
            ServerVersion.V026 -> userApi.deleteAccessTokenV026(segment, token)
            // On v0.31 `token` is the id segment from the resource name, not the secret.
            ServerVersion.V031 -> userApi.deleteAccessTokenV031(segment, token)
        }
    }

    override suspend fun listWebhooks(name: String): List<Webhook> {
        val segment = userSegment(name)
        return when (version) {
            ServerVersion.V024 ->
                webhookApi.listWebhooks("users/$segment").webhooks.map { it.toDomain() }
            ServerVersion.V025, ServerVersion.V026 ->
                webhookApi.listWebhooksV025(segment).webhooks.map { it.toDomain() }
            ServerVersion.V031 ->
                webhookApi.listWebhooksV031(segment).webhooks.map { it.toDomain() }
        }
    }

    override suspend fun createWebhook(owner: String, title: String, url: String): Webhook =
        when (version) {
            // The top-level endpoint exists up to v0.26; v0.31 nests creation under
            // the user and renames the title field.
            ServerVersion.V024, ServerVersion.V025, ServerVersion.V026 ->
                webhookApi.createWebhook(CreateWebhookRequest(title, url)).toDomain()
            ServerVersion.V031 ->
                webhookApi.createWebhookV031(
                    userSegment(owner),
                    CreateWebhookV031Request(url = url, displayName = title),
                ).toDomain()
        }

    override suspend fun deleteWebhook(webhook: Webhook) {
        when (version) {
            ServerVersion.V024, ServerVersion.V025, ServerVersion.V026 ->
                webhookApi.deleteWebhook(webhook.id.toIntOrNull() ?: return)
            // v0.31 addresses the webhook by its full resource name.
            ServerVersion.V031 -> {
                val parts = webhook.resourceName.split("/")
                if (parts.size == 4 && parts[0] == "users" && parts[2] == "webhooks") {
                    webhookApi.deleteWebhookV031(parts[1], parts[3])
                }
            }
        }
    }
}
