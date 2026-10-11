package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.InboxMessage
import com.whtis.memosly.core.model.InboxStatus
import com.whtis.memosly.core.model.InboxType
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.InboxApi
import com.whtis.memosly.core.network.dto.UpdateInboxRequest
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxRepositoryImpl @Inject constructor(
    private val inboxApi: InboxApi,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : InboxRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    private fun currentUserSegment(): String {
        val user = authRepository.currentUser.value
            ?: throw IllegalStateException("Not authenticated")
        return user.name.substringAfterLast("/")
    }

    override suspend fun listInbox(): List<InboxMessage> =
        when (version) {
            ServerVersion.V031 ->
                inboxApi.listNotificationsV031(currentUserSegment())
                    .notifications.map { it.toDomain() }
            else -> inboxApi.listInbox().inboxes.map { it.toDomain() }
        }

    override suspend fun updateInboxStatus(name: String, status: String): InboxMessage {
        val id = name.substringAfterLast("/")
        return when (version) {
            ServerVersion.V031 -> {
                // v0.31 has no READ state — a notification is UNREAD until it is
                // ARCHIVED. "Mark as read" therefore has nothing to persist; archive
                // stays the one real transition.
                if (status != "ARCHIVED") {
                    return InboxMessage(
                        name = name,
                        sender = "",
                        receiver = "",
                        status = InboxStatus.UNREAD,
                        createTime = "",
                        type = InboxType.UNKNOWN,
                        activityId = null,
                    )
                }
                val userSegment = name.split("/").getOrNull(1) ?: currentUserSegment()
                inboxApi.updateNotificationV031(userSegment, id, UpdateInboxRequest(status))
                    .toDomain()
            }
            else -> inboxApi.updateInbox(id, UpdateInboxRequest(status)).toDomain()
        }
    }
}
