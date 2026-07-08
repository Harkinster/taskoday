package com.example.taskoday.di

import com.example.taskoday.BuildConfig
import com.example.taskoday.data.remote.ApiClient
import com.example.taskoday.data.remote.ApiPathPrefixInterceptor
import com.example.taskoday.data.remote.auth.AuthApi
import com.example.taskoday.data.remote.auth.AuthHeaderInterceptor
import com.example.taskoday.data.remote.auth.SecureTokenStorage
import com.example.taskoday.data.remote.auth.TokenStorage
import com.example.taskoday.data.remote.auth.UnauthorizedInterceptor
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.data.remote.missions.MissionsApi
import com.example.taskoday.data.remote.pairing.PairingApi
import com.example.taskoday.data.remote.planning.PlanningApi
import com.example.taskoday.data.remote.profile.ProfileApi
import com.example.taskoday.data.remote.quests.QuestsApi
import com.example.taskoday.data.remote.rewards.RewardsApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTokenStorage(impl: SecureTokenStorage): TokenStorage = impl

    @Provides
    @Singleton
    fun provideBaseUrl(): String = BuildConfig.TASKODAY_BASE_URL

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            sensitiveHttpHeaders.forEach(::redactHeader)
            level = taskodayHttpLogLevel(isDebug = BuildConfig.DEBUG)
        }

    @Provides
    @Singleton
    fun provideApiPathPrefixInterceptor(baseUrl: String): ApiPathPrefixInterceptor =
        ApiPathPrefixInterceptor(baseUrl.toHttpUrl())

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiPathPrefixInterceptor: ApiPathPrefixInterceptor,
        authHeaderInterceptor: AuthHeaderInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(apiPathPrefixInterceptor)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        baseUrl: String,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideApiClient(retrofit: Retrofit): ApiClient = ApiClient(retrofit)

    @Provides
    @Singleton
    fun provideAuthApi(apiClient: ApiClient): AuthApi = apiClient.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providePlanningApi(apiClient: ApiClient): PlanningApi = apiClient.create(PlanningApi::class.java)

    @Provides
    @Singleton
    fun provideChildrenApi(apiClient: ApiClient): ChildrenApi = apiClient.create(ChildrenApi::class.java)

    @Provides
    @Singleton
    fun provideNestApi(apiClient: ApiClient): NestApi = apiClient.create(NestApi::class.java)

    @Provides
    @Singleton
    fun provideMissionsApi(apiClient: ApiClient): MissionsApi = apiClient.create(MissionsApi::class.java)

    @Provides
    @Singleton
    fun provideQuestsApi(apiClient: ApiClient): QuestsApi = apiClient.create(QuestsApi::class.java)

    @Provides
    @Singleton
    fun provideRewardsApi(apiClient: ApiClient): RewardsApi = apiClient.create(RewardsApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(apiClient: ApiClient): ProfileApi = apiClient.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun providePairingApi(apiClient: ApiClient): PairingApi = apiClient.create(PairingApi::class.java)
}

internal val sensitiveHttpHeaders = listOf("Authorization")

internal fun taskodayHttpLogLevel(isDebug: Boolean): HttpLoggingInterceptor.Level =
    if (isDebug) {
        HttpLoggingInterceptor.Level.BASIC
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
