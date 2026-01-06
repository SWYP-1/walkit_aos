package swyp.team.walkit.data.remote.interceptor

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import swyp.team.walkit.data.remote.auth.TokenProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 401 Unauthorized 응답 시 토큰 갱신을 처리하는 Authenticator
 * 
 * 동작 방식:
 * 1. 401 응답 감지
 * 2. Refresh Token으로 새로운 Access Token 요청 (향후 구현)
 * 3. 새 토큰으로 원래 요청 재시도
 * 4. 실패 시 null 반환 (재시도 안 함)
 * 
 * 주의: 현재는 토큰 갱신 로직이 없으므로 null 반환
 * 향후 refresh token API가 추가되면 구현 필요
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // 401이 아니면 처리하지 않음
        if (response.code != 401) {
            return null
        }

        // 이미 재시도한 경우 무한 루프 방지
        if (responseCount(response) >= 2) {
            Timber.w("토큰 갱신 실패: 최대 재시도 횟수 초과")
            return null
        }

        val refreshToken = tokenProvider.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            Timber.w("리프레시 토큰이 없습니다. 로그인 필요")
            return null
        }

        // 현재는 토큰 갱신 API가 없으므로 null 반환
        // 향후 refresh token API가 추가되면 여기서 갱신 처리
        Timber.w("401 응답 감지. 토큰 갱신 필요하지만 현재는 미구현. 로그인 화면으로 이동 필요")
        return null

        // 향후 구현 예시:
        // try {
        //     val refreshTokenApi = // 별도의 Retrofit 인스턴스 사용 (순환 참조 방지)
        //     val newTokenResponse = runBlocking {
        //         refreshTokenApi.refreshToken(RefreshTokenRequest(refreshToken))
        //     }
        //     tokenProvider.updateTokens(newTokenResponse.accessToken, newTokenResponse.refreshToken)
        //     return response.request.newBuilder()
        //         .header("Authorization", "Bearer ${newTokenResponse.accessToken}")
        //         .build()
        // } catch (t: Throwable) {
        //     Timber.e(t, "토큰 갱신 실패")
        //     return null
        // }
    }

    /**
     * 응답 체인에서 재시도 횟수 계산
     */
    private fun responseCount(response: Response): Int {
        var result = 1
        var current = response.priorResponse
        while (current != null) {
            result++
            current = current.priorResponse
        }
        return result
    }
}










