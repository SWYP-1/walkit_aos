package team.swyp.sdu.data.remote.auth

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.api.auth.AuthApi
import team.swyp.sdu.data.api.auth.AuthTokenResponse
import team.swyp.sdu.data.api.auth.SocialLoginRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 원격 데이터 소스
 * AuthApi 호출을 래핑하고 에러 처리
 */
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val authApi: AuthApi,
) {
    /**
     * 카카오 로그인
     * @param kakaoAccessToken 카카오 SDK에서 받은 액세스 토큰
     * @return 서버에서 받은 토큰 응답
     */
    suspend fun loginWithKakao(kakaoAccessToken: String): Result<AuthTokenResponse> {
        return try {
            val response = authApi.loginWithKakao(
                SocialLoginRequest(kakaoAccessToken)
            )
            Timber.i("카카오 로그인 성공")
            Result.Success(response)
        } catch (e: Exception) {
            Timber.e(e, "카카오 로그인 실패")
            Result.Error(e, "카카오 로그인에 실패했습니다: ${e.message}")
        }
    }

    /**
     * 네이버 로그인
     * @param naverAccessToken 네이버 SDK에서 받은 액세스 토큰
     * @return 서버에서 받은 토큰 응답
     */
    suspend fun loginWithNaver(naverAccessToken: String): Result<AuthTokenResponse> {
        return try {
            val response = authApi.loginWithNaver(
                SocialLoginRequest(naverAccessToken)
            )
            Timber.i("네이버 로그인 성공")
            Result.Success(response)
        } catch (e: Exception) {
            Timber.e(e, "네이버 로그인 실패")
            Result.Error(e, "네이버 로그인에 실패했습니다: ${e.message}")
        }
    }
}






