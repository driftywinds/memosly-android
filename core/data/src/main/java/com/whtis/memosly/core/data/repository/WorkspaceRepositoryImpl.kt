package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.IdentityProvider
import com.whtis.memosly.core.model.WorkspaceProfile
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.IdentityProviderApi
import com.whtis.memosly.core.network.api.WorkspaceApi
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val workspaceApi: WorkspaceApi,
    private val identityProviderApi: IdentityProviderApi,
    private val tokenManager: TokenManager,
) : WorkspaceRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    override suspend fun getWorkspaceProfile(): WorkspaceProfile =
        when (version) {
            // v0.31: /workspace/profile is gone; /instance/profile reports the
            // version and first admin instead of owner/mode.
            ServerVersion.V031 -> workspaceApi.getInstanceProfileV031().toDomain()
            else -> workspaceApi.getWorkspaceProfile().toDomain()
        }

    override suspend fun listIdentityProviders(): List<IdentityProvider> =
        when (version) {
            ServerVersion.V031 ->
                identityProviderApi.listIdentityProvidersV031().identityProviders.map { it.toDomain() }
            else ->
                identityProviderApi.listIdentityProviders().identityProviders.map { it.toDomain() }
        }
}
