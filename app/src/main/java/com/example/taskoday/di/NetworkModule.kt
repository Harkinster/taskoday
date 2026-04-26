package com.example.taskoday.di

import com.example.taskoday.BuildConfig
import com.example.taskoday.data.remote.ApiClient
import com.example.taskoday.data.remote.auth.AuthApi
import com.example.taskoday.data.remote.auth.AuthHeaderInterceptor
import com.example.taskoday.data.remote.auth.SecureTokenStorage
import com.example.taskoday.data.remote.auth.TokenStorage
import com.example.taskoday.data.remote.auth.UnauthorizedInterceptor
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.missions.MissionsApi
import com.example.taskoday.data.remote.pairing.PairingApi
import com.example.taskoday.data.remote.planning.PlanningApi
import com.example.taskoday.data.remote.profile.ProfileApi
import com.example.taskoday.data.remote.quests.QuestsApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
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
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authHeaderInterceptor: AuthHeaderInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(loggingInterceptor)
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
    fun provideMissionsApi(apiClient: ApiClient): MissionsApi = apiClient.create(MissionsApi::class.java)

    @Provides
    @Singleton
    fun provideQuestsApi(apiClient: ApiClient): QuestsApi = apiClient.create(QuestsApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(apiClient: ApiClient): ProfileApi = apiClient.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun providePairingApi(apiClient: ApiClient): PairingApi = apiClient.create(PairingApi::class.java)
}
