package com.whtis.memosly.core.network.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.whtis.memosly.core.network.MemosCookies
import com.whtis.memosly.core.network.dto.CreateAccessTokenV031Request
import com.whtis.memosly.core.network.dto.CreateMemoRequest
import com.whtis.memosly.core.network.dto.CreateWebhookV031Request
import com.whtis.memosly.core.network.dto.MemoRelationAdapter
import com.whtis.memosly.core.network.dto.ReactionFields
import com.whtis.memosly.core.network.dto.RefreshTokenV031Request
import com.whtis.memosly.core.network.dto.UpdateInboxRequest
import com.whtis.memosly.core.network.dto.UpsertReactionRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Pins the v0.31 wire contract against a real schema dump of memos v0.31.0.
 *
 * v0.31 changed several paths (workspace→instance, identityProviders→identity-providers,
 * inboxes→user notifications), renamed payload fields (webhooks lost `id`, PATs lost the
 * token secret), and moved auth to a 15-minute access token plus a rotating refresh
 * cookie. Every one of these failed silently or loudly when the server was upgraded past
 * what the app knew, so they are pinned here: a wrong path answers 404 and a wrong
 * payload shape throws in Moshi, and both used to surface as empty screens.
 */
class V031EndpointTest {

    private lateinit var server: MockWebServer
    private lateinit var moshi: Moshi
    private lateinit var authApi: AuthApi
    private lateinit var inboxApi: InboxApi
    private lateinit var userApi: UserApi
    private lateinit var webhookApi: WebhookApi
    private lateinit var workspaceApi: WorkspaceApi
    private lateinit var identityProviderApi: IdentityProviderApi
    private lateinit var memoApi: MemoApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        moshi = Moshi.Builder()
            .add(MemoRelationAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
        authApi = retrofit.create(AuthApi::class.java)
        inboxApi = retrofit.create(InboxApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
        webhookApi = retrofit.create(WebhookApi::class.java)
        workspaceApi = retrofit.create(WorkspaceApi::class.java)
        identityProviderApi = retrofit.create(IdentityProviderApi::class.java)
        memoApi = retrofit.create(MemoApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `refresh posts to auth refresh with the cookie in Grpc-Metadata-Cookie`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"accessToken":"new-token","expiresAt":"2026-10-11T06:22:48Z"}
                """.trimIndent()
            )
        )

        val response = runBlocking {
            authApi.refreshTokenV031(
                refreshCookie = MemosCookies.asRequestHeader("cookie-value"),
                request = RefreshTokenV031Request(),
            )
        }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/refresh", request.path)
        assertEquals("memos_refresh=cookie-value", request.getHeader("Grpc-Metadata-Cookie"))
        assertEquals("new-token", response.body()?.accessToken)
    }

    @Test
    fun `memos cookies are read from Grpc-Metadata-Set-Cookie`() {
        val headers = okhttp3.Headers.headersOf(
            "Grpc-Metadata-Set-Cookie",
            "memos_refresh=the-cookie; Path=/; HttpOnly; Expires=Tue, 10 Nov 2026 05:50:38 GMT; SameSite=Lax",
        )
        assertEquals("the-cookie", MemosCookies.fromResponseHeaders(headers))
    }

    @Test
    fun `notifications are listed under the user resource`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"notifications":[{"name":"users/tester/notifications/1","sender":"users/bob",
                "status":"UNREAD","createTime":"2026-10-11T05:58:29Z","type":"MEMO_COMMENT"}]}
                """.trimIndent()
            )
        )

        val response = runBlocking { inboxApi.listNotificationsV031("tester") }

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/users/tester/notifications", request.path)
        val message = response.notifications.single()
        assertEquals("users/tester/notifications/1", message.name)
        assertEquals("UNREAD", message.status)
        assertEquals("MEMO_COMMENT", message.type)
    }

    @Test
    fun `notification status is patched on the notification resource`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"name":"users/tester/notifications/1","status":"ARCHIVED"}"""
            )
        )

        runBlocking { inboxApi.updateNotificationV031("tester", "1", UpdateInboxRequest("ARCHIVED")) }

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/users/tester/notifications/1", request.path)
        assertEquals("""{"status":"ARCHIVED"}""", request.body.readUtf8())
    }

    @Test
    fun `personal access tokens list returns the v031 payload without a token secret`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"personalAccessTokens":[{"name":"users/tester/personalAccessTokens/uuid-1",
                "description":"app token","createdAt":"2026-10-11T05:56:11Z"}],
                "nextPageToken":""}
                """.trimIndent()
            )
        )

        val response = runBlocking { userApi.listAccessTokensV031("tester") }

        assertEquals("/api/v1/users/tester/personalAccessTokens", server.takeRequest().path)
        val pat = response.personalAccessTokens.single()
        assertEquals("users/tester/personalAccessTokens/uuid-1", pat.name)
        assertEquals("app token", pat.description)
    }

    @Test
    fun `personal access token creation posts a description only`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"personalAccessToken":{"name":"users/tester/personalAccessTokens/uuid-1"},
                "token":"memos_pat_secret"}
                """.trimIndent()
            )
        )

        runBlocking { userApi.createAccessTokenV031("tester", CreateAccessTokenV031Request("app token")) }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/users/tester/personalAccessTokens", request.path)
        assertEquals("""{"description":"app token"}""", request.body.readUtf8())
    }

    @Test
    fun `personal access token deletion uses the resource name segment`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        runBlocking { userApi.deleteAccessTokenV031("tester", "uuid-1") }

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/users/tester/personalAccessTokens/uuid-1", request.path)
    }

    @Test
    fun `webhooks parse the v031 payload without the legacy id`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"webhooks":[{"name":"users/tester/webhooks/9630d30af3a17d58",
                "url":"http://example.com/hook","displayName":"myhook"}]}
                """.trimIndent()
            )
        )

        val response = runBlocking { webhookApi.listWebhooksV031("tester") }

        assertEquals("/api/v1/users/tester/webhooks", server.takeRequest().path)
        val webhook = response.webhooks.single()
        assertEquals("myhook", webhook.displayName)
        assertEquals("9630d30af3a17d58", webhook.name.substringAfterLast("/"))
    }

    @Test
    fun `webhook creation nests under the user with the title as displayName`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"name":"users/tester/webhooks/abc","url":"http://example.com/hook","displayName":"myhook"}"""
            )
        )

        runBlocking {
            webhookApi.createWebhookV031(
                "tester",
                CreateWebhookV031Request(url = "http://example.com/hook", displayName = "myhook"),
            )
        }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/users/tester/webhooks", request.path)
        assertEquals(
            """{"url":"http://example.com/hook","displayName":"myhook"}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `webhook deletion uses the full resource name segments`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        runBlocking { webhookApi.deleteWebhookV031("tester", "9630d30af3a17d58") }

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/users/tester/webhooks/9630d30af3a17d58", request.path)
    }

    @Test
    fun `instance profile replaces the removed workspace profile`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"version":"0.31.0","demo":false,"instanceUrl":"",
                "admin":{"name":"users/tester","role":"ADMIN","username":"tester"},
                "commit":"abc","needsSetup":false,"accessMode":"INSTANCE_ACCESS_MODE_PRIVATE"}
                """.trimIndent()
            )
        )

        val profile = runBlocking { workspaceApi.getInstanceProfileV031() }

        assertEquals("/api/v1/instance/profile", server.takeRequest().path)
        assertEquals("0.31.0", profile.version)
        assertEquals("tester", profile.admin?.username)
    }

    @Test
    fun `identity providers moved to the hyphenated path`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"identityProviders":[]}"""))

        runBlocking { identityProviderApi.listIdentityProvidersV031() }

        assertEquals("/api/v1/identity-providers", server.takeRequest().path)
    }

    @Test
    fun `comments carry the parent visibility and space`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"name":"memos/comment-1","content":"hi","visibility":"PROTECTED"}"""
            )
        )

        runBlocking {
            memoApi.createMemoComment(
                "parent-uid",
                CreateMemoRequest(content = "hi", visibility = "PROTECTED", space = "spaces/team"),
            )
        }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/memos/parent-uid/comments", request.path)
        assertEquals(
            """{"content":"hi","visibility":"PROTECTED","space":"spaces/team"}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `reactions upsert without contentId on v031`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"name":"memos/m1/reactions/1","creator":"users/tester","reactionType":"👍"}"""
            )
        )

        runBlocking {
            memoApi.upsertMemoReaction("m1", UpsertReactionRequest(ReactionFields(contentId = null, reactionType = "👍")))
        }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/memos/m1/reactions", request.path)
        val body = request.body.readUtf8()
        assertTrue("v0.31 reactions must not send contentId, got: $body", !body.contains("contentId"))
        assertTrue("reactionType missing from: $body", body.contains("reactionType"))
    }

    @Test
    fun `memo parses the space field`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"name":"memos/m1","content":"hi","visibility":"SPACE","space":"spaces/team",
                "state":"NORMAL","creator":"users/tester"}
                """.trimIndent()
            )
        )

        val memo = runBlocking { memoApi.getMemo("m1") }

        assertEquals("spaces/team", memo.space)
        assertEquals("SPACE", memo.visibility)
    }

    @Test
    fun `user stats keep the getStats suffix and report a total count`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"name":"users/tester/stats","tagCount":{"tagone":2},
                "memoCreatedTimestamps":["2026-10-11T05:56:10Z"],"totalMemoCount":1}
                """.trimIndent()
            )
        )

        val stats = runBlocking { userApi.getUserStatsV026("tester") }

        assertEquals("/api/v1/users/tester:getStats", server.takeRequest().path)
        assertEquals(1, stats.totalMemoCount)
        assertEquals(mapOf("tagone" to 2), stats.tagCount)
    }
}
