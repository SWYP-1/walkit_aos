package swyp.team.walkit.di

import swyp.team.walkit.data.api.auth.AuthApi
import swyp.team.walkit.data.api.auth.CharacterApi
import swyp.team.walkit.data.api.cosmetic.CosmeticItemApi
import swyp.team.walkit.data.remote.auth.CharacterRemoteDataSource
import swyp.team.walkit.data.api.follower.FollowerApi
import swyp.team.walkit.data.api.goal.GoalApi
import swyp.team.walkit.data.api.mission.MissionApi
import swyp.team.walkit.data.api.user.UserApi
import swyp.team.walkit.data.remote.auth.TokenProvider
import swyp.team.walkit.data.remote.auth.TokenProviderImpl
import swyp.team.walkit.data.remote.interceptor.AuthInterceptor
import swyp.team.walkit.data.remote.interceptor.TokenAuthenticator
import swyp.team.walkit.core.AuthEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import swyp.team.walkit.BuildConfig
import swyp.team.walkit.data.api.walking.WalkApi
import swyp.team.walkit.data.api.home.HomeApi
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
            // 릴리즈 빌드에서는 로깅 비활성화 (보안)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider,
    ): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        @ApplicationContext context: Context,
        tokenProvider: TokenProvider,
        authEventBus: AuthEventBus,
    ): TokenAuthenticator = TokenAuthenticator(context, tokenProvider, authEventBus)

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
    ): swyp.team.walkit.data.api.notification.NotificationApi =
        retrofit.create(swyp.team.walkit.data.api.notification.NotificationApi::class.java)

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