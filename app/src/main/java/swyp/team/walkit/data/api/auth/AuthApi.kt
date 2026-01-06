package swyp.team.walkit.data.api.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/auth/naver")
    suspend fun loginWithNaver(
        @Body request: SocialLoginRequest
    ): AuthTokenResponse

    @POST("/auth/kakao")
    suspend fun loginWithKakao(
        @Body request: SocialLoginRequest
    ): AuthTokenResponse
}
