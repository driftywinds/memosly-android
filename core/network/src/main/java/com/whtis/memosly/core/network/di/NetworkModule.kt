package com.whtis.memosly.core.network.di

import com.whtis.memosly.core.network.AuthInterceptor
import com.whtis.memosly.core.network.PLACEHOLDER_HOST
import com.whtis.memosly.core.network.ServerUrlInterceptor
import com.whtis.memosly.core.network.TokenAuthenticator
import com.whtis.memosly.core.network.api.*
import com.whtis.memosly.core.network.dto.MemoRelationAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(MemoRelationAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Bare client with no memos interceptors or authenticator. Used by [com.whtis.memosly.core.network.TokenRefresher]
     * so a token refresh can never recurse into the client it is refreshing for.
     */
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        serverUrlInterceptor: ServerUrlInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(serverUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
        )
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://$PLACEHOLDER_HOST/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMemoApi(retrofit: Retrofit): MemoApi =
        retrofit.create(MemoApi::class.java)

    @Provides
    @Singleton
    fun provideResourceApi(retrofit: Retrofit): ResourceApi =
        retrofit.create(ResourceApi::class.java)

    @Provides
    @Singleton
    fun provideTagApi(retrofit: Retrofit): TagApi =
        retrofit.create(TagApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideWorkspaceApi(retrofit: Retrofit): WorkspaceApi =
        retrofit.create(WorkspaceApi::class.java)

    @Provides
    @Singleton
    fun provideIdentityProviderApi(retrofit: Retrofit): IdentityProviderApi =
        retrofit.create(IdentityProviderApi::class.java)

    @Provides
    @Singleton
    fun provideInboxApi(retrofit: Retrofit): InboxApi =
        retrofit.create(InboxApi::class.java)

    @Provides
    @Singleton
    fun provideWebhookApi(retrofit: Retrofit): WebhookApi =
        retrofit.create(WebhookApi::class.java)
}
