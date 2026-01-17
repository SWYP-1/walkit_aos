package swyp.team.walkit.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import swyp.team.walkit.data.remote.auth.TokenProvider
import swyp.team.walkit.data.remote.exception.AuthExpiredException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 인증 토큰을 요청 헤더에 추가하는 인터셉터
 *
 * 역할: AccessToken 헤더 추가 + 401 응답 감지 시 토큰 갱신
 * (서버가 WWW-Authenticate 헤더를 보내지 않아 Authenticator가 동작하지 않음)
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
    @Named("walkit") private val retrofitProvider: Provider<Retrofit>,
) : Interceptor {

    // 🔒 AuthInterceptor 레벨 동시성 제어
    // 여러 401 요청이 동시에 와도 하나의 재시도만 수행
    private val interceptorMutex = kotlinx.coroutines.sync.Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 인증이 필요 없는 요청은 제외
        if (request.url.encodedPath.contains("/auth/")) {
            return chain.proceed(request)
        }

        // 캐시된 토큰 가져오기 및 Authorization 헤더 추가
        val accessToken = tokenProvider.getAccessToken()
        val newRequest = if (!accessToken.isNullOrBlank()) {
            Timber.d("AuthInterceptor - Authorization 헤더 추가")
            request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            Timber.w("AuthInterceptor - 액세스 토큰 없음")
            request
        }

        val response = chain.proceed(newRequest)

        // 401 응답 감지 시 토큰 갱신 시도
        if (response.code == 401) {
            Timber.e("AuthInterceptor - 401 감지! 코드: ${response.code}, URL: ${request.url}")
            response.close() // 기존 응답 닫기

            return runBlocking {
                // 🔒 여러 401 요청 동시 접근 방지 (단순 동기화)
                interceptorMutex.withLock {
                    try {
                        // 📞 TokenProvider에 refresh 요청 (단일 refresh 보장)
                        val authApi = retrofitProvider.get().create(swyp.team.walkit.data.api.auth.AuthApi::class.java)
                        val refreshSuccess = tokenProvider.refreshTokensOn401(authApi)

                        if (refreshSuccess) {
                            Timber.d("AuthInterceptor - 토큰 갱신 성공, 원래 요청 재시도")
                            val newAccessToken = tokenProvider.getAccessToken()
                            if (newAccessToken.isNullOrBlank()) {
                                Timber.e("AuthInterceptor - 갱신 성공했는데 액세스 토큰이 없음!")
                                throw AuthExpiredException("Token refresh succeeded but no access token available")
                            }

                            // 🔄 새로운 토큰으로 원래 요청 재시도
                            val retryRequest = request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                            val retryResponse = chain.proceed(retryRequest)

                            // ⚠️ 재시도했는데 또 401이면 인증 만료 처리
                            if (retryResponse.code == 401) {
                                Timber.e("AuthInterceptor - 재시도했는데 또 401! 인증 만료")
                                retryResponse.close()
                                throw AuthExpiredException("Authentication expired - request still fails after token refresh")
                            }

                            return@runBlocking retryResponse
                        } else {
                            Timber.e("AuthInterceptor - 토큰 갱신 실패")
                            throw AuthExpiredException("Token refresh failed: authentication expired")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "AuthInterceptor - 토큰 갱신 중 예외")
                        throw AuthExpiredException("Token refresh failed: ${e.message}")
                    }
                }
            }
        }

        return response
    }
}


