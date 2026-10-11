package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.InboxMessageDto
import com.whtis.memosly.core.network.dto.ListInboxResponse
import com.whtis.memosly.core.network.dto.ListUserNotificationsResponseV031
import com.whtis.memosly.core.network.dto.UpdateInboxRequest
import com.whtis.memosly.core.network.dto.UserNotificationDtoV031
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface InboxApi {
    @GET("api/v1/inboxes")
    suspend fun listInbox(): ListInboxResponse

    @PATCH("api/v1/inboxes/{name}")
    suspend fun updateInbox(
        @Path("name") name: String,
        @Body request: UpdateInboxRequest,
    ): InboxMessageDto

    // v0.31: notifications replaced inboxes and live under the user resource
    @GET("api/v1/users/{id}/notifications")
    suspend fun listNotificationsV031(@Path("id") id: String): ListUserNotificationsResponseV031

    @PATCH("api/v1/users/{id}/notifications/{notificationId}")
    suspend fun updateNotificationV031(
        @Path("id") id: String,
        @Path("notificationId") notificationId: String,
        @Body request: UpdateInboxRequest,
    ): UserNotificationDtoV031
}
