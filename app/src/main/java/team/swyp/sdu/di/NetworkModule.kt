package team.swyp.sdu.di

import team.swyp.sdu.data.api.auth.AuthApi
import team.swyp.sdu.data.api.auth.CharacterApi
import team.swyp.sdu.data.api.cosmetic.CosmeticItemApi
import team.swyp.sdu.data.remote.auth.CharacterRemoteDataSource
import team.swyp.sdu.data.api.follower.FollowerApi
import team.swyp.sdu.data.api.goal.GoalApi
import team.swyp.sdu.data.api.mission.MissionApi
import team.swyp.sdu.data.api.user.UserApi
import team.swyp.sdu.data.remote.auth.TokenProvider
import team.swyp.sdu.data.remote.auth.TokenProviderImpl
import team.swyp.sdu.data.remote.interceptor.AuthInterceptor
import team.swyp.sdu.data.remote.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import team.swyp.sdu.data.api.walking.WalkApi
import team.swyp.sdu.data.api.home.HomeApi
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false // null 필드는 JSON에서 제외 (서버에는 true/false만 전송)
            isLenient = true
            coerceInputValues = true // null이 올 수 있는 필드에 강제 타입 변환 허용
        }

    @Provides
    @Singleton
    fun provideTokenProvider(
        tokenProviderImpl: TokenProviderImpl,
    ): TokenProvider = tokenProviderImpl

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider,
    ): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenProvider: TokenProvider,
    ): TokenAuthenticator = TokenAuthenticator(tokenProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor) // 로깅 전에 추가 (순서 중요)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator) // 401 응답 처리
            .followRedirects(false) // API에서는 리다이렉트 따라가지 않음 (중요!)
            .followSslRedirects(false)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("walkit")
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://walkit-shop-swyp-11.shop/") // walkit server
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(
        @Named("walkit") retrofit: Retrofit,
    ): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCharacterApi(
        @Named("walkit") retrofit: Retrofit,
    ): CharacterApi = retrofit.create(CharacterApi::class.java)

    @Provides
    @Singleton
    fun provideCharacterRemoteDataSource(
        characterApi: CharacterApi,
    ): CharacterRemoteDataSource = CharacterRemoteDataSource(characterApi)

    @Provides
    @Singleton
    fun provideUserApi(
        @Named("walkit") retrofit: Retrofit,
    ): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideMissionApi(
        @Named("walkit") retrofit: Retrofit,
    ): MissionApi = retrofit.create(MissionApi::class.java)

    @Provides
    @Singleton
    fun provideGoalApi(
        @Named("walkit") retrofit: Retrofit,
    ): GoalApi = retrofit.create(GoalApi::class.java)

    @Provides
    @Singleton
    fun provideWalkApi(
        @Named("walkit") retrofit: Retrofit,
    ): WalkApi = retrofit.create(WalkApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(
        @Named("walkit") retrofit: Retrofit,
    ): team.swyp.sdu.data.api.notification.NotificationApi =
        retrofit.create(team.swyp.sdu.data.api.notification.NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideHomeApi(
        @Named("walkit") retrofit: Retrofit,
    ): HomeApi = retrofit.create(HomeApi::class.java)

    @Provides
    @Singleton
    fun provideFollowerApi(
        @Named("walkit") retrofit: Retrofit,
    ): FollowerApi = retrofit.create(FollowerApi::class.java)

    @Provides
    @Singleton
    fun provideCosmeticItemApi(
        @Named("walkit") retrofit: Retrofit,
    ): CosmeticItemApi = retrofit.create(CosmeticItemApi::class.java)

    @Provides
    @Singleton
    @Named("image")
    fun provideImageOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor) // AuthInterceptor 제외 - 이미지 다운로드용
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
}