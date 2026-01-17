package swyp.team.walkit.data.api.auth

import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.serialization.Serializable
import retrofit2.Response

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

interface AuthApi {

    @POST("/auth/naver")
    suspend fun loginWithNaver(
        @Body request: SocialLoginRequest
    ): AuthTokenResponse

    @POST("/auth/kakao")
    suspend fun loginWithKakao(
        @Body request: SocialLoginRequest
    ): AuthTokenResponse

    @POST("/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthTokenResponse>

    @POST("/auth/logout")
    suspend fun logout(
    ): Response<Unit>
}
