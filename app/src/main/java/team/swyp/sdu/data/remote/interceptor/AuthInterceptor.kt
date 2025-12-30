package team.swyp.sdu.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import team.swyp.sdu.data.remote.auth.TokenProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 토큰을 요청 헤더에 추가하는 인터셉터
 * 
 * 주의: runBlocking을 사용하지 않고 TokenProvider의 캐시된 토큰 사용
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    private val lock = Any()
    @Volatile
    private var isRefreshing = false
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 인증이 필요 없는 요청은 제외
        // 1. 로그인 API는 토큰 불필요
        // 2. 공개 API는 필요시 추가
        if (request.url.encodedPath.contains("/auth/")) {
            return chain.proceed(request)
        }

        // 캐시된 토큰 가져오기 (동기, runBlocking 없음)
        val accessToken = tokenProvider.getAccessToken()
        Timber.d("AuthInterceptor - 요청 URL: ${request.url}, 토큰 존재: ${!accessToken.isNullOrBlank()}")

        val newRequest = if (!accessToken.isNullOrBlank()) {
            Timber.d("AuthInterceptor - Authorization 헤더 추가: Bearer ${accessToken.take(20)}...")
            request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            Timber.w("액세스 토큰이 없습니다. 요청: ${request.url}")
            request
        }

        val response = chain.proceed(newRequest)

        // 응답 상태 로깅 (디버깅용)
        Timber.d("AuthInterceptor - 응답 상태: ${response.code}, Content-Type: ${response.header("Content-Type")}")

        // 인증 실패 감지: 401, 302 리다이렉트 또는 HTML 응답
        val isAuthFailure = response.code == 401 ||
                           (response.code == 302 && response.header("Location")?.contains("/login") == true) ||
                           (response.header("Content-Type")?.contains("text/html") == true && !request.url.encodedPath.contains("/auth/"))

        if (isAuthFailure) {
            Timber.e("AuthInterceptor - 인증 실패 감지! 코드: ${response.code}, Location: ${response.header("Location")}")

            // 동시성 제어: 한 번에 하나의 토큰 갱신만 수행
            synchronized(lock) {
                if (!isRefreshing) {
                    isRefreshing = true
                    try {
                        // 토큰 갱신 시도
                        val refreshSuccess = runBlocking { refreshToken(chain) }

                        if (refreshSuccess) {
                            Timber.d("AuthInterceptor - 토큰 갱신 성공, 원래 요청 재시도")
                            // 새 토큰으로 원래 요청 재시도
                            val newAccessToken = tokenProvider.getAccessToken()
                            val retryRequest = request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                            response.close() // 기존 응답 닫기
                            return chain.proceed(retryRequest)
                        } else {
                            Timber.e("AuthInterceptor - 토큰 갱신 실패")
                            response.close()
                            // TODO: 로그인 화면으로 이동하는 이벤트 발생 필요
                            return response
                        }
                    } finally {
                        isRefreshing = false
                    }
                } else {
                    Timber.d("AuthInterceptor - 다른 스레드에서 토큰 갱신 중, 대기 후 재시도")
                    // 다른 스레드가 갱신 중이면 잠시 대기
                    Thread.sleep(100)

                    // 갱신 완료된 새 토큰으로 재시도
                    val newAccessToken = tokenProvider.getAccessToken()
                    if (!newAccessToken.isNullOrBlank()) {
                        val retryRequest = request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                        response.close()
                        return chain.proceed(retryRequest)
                    }
                }
            }
        }

        return response
    }

    private suspend fun refreshToken(chain: Interceptor.Chain): Boolean {
        return try {
            val refreshToken = tokenProvider.getRefreshToken()

            if (refreshToken.isNullOrBlank()) {
                Timber.e("AuthInterceptor - Refresh token이 없습니다")
                tokenProvider.clearTokens()
                return false
            }

            Timber.d("AuthInterceptor - Refresh token으로 토큰 갱신 요청")

            // Refresh API 요청 생성
            val jsonBody = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString()

            val originalRequest = chain.request()
            val baseUrl = "${originalRequest.url.scheme}://${originalRequest.url.host}"

            val refreshRequest = Request.Builder()
                .url("$baseUrl/auth/refresh")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val refreshResponse = chain.proceed(refreshRequest)

            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                Timber.d("AuthInterceptor - Refresh 응답: $responseBody")

                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val newAccessToken = jsonResponse.optString("accessToken")
                    val newRefreshToken = jsonResponse.optString("refreshToken")

                    if (newAccessToken.isNotBlank()) {
                        // 새 토큰 저장 (updateTokens 사용)
                        tokenProvider.updateTokens(newAccessToken, newRefreshToken.takeIf { it.isNotBlank() })
                        Timber.d("AuthInterceptor - 새 토큰 저장 완료")
                        refreshResponse.close()
                        return true
                    } else {
                        Timber.e("AuthInterceptor - 응답에 accessToken이 없습니다")
                        tokenProvider.clearTokens()
                        refreshResponse.close()
                        return false
                    }
                } else {
                    Timber.e("AuthInterceptor - Refresh 응답 body가 null입니다")
                    tokenProvider.clearTokens()
                    refreshResponse.close()
                    return false
                }
            } else {
                Timber.e("AuthInterceptor - Refresh 실패: ${refreshResponse.code}")
                tokenProvider.clearTokens()
                refreshResponse.close()
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "AuthInterceptor - 토큰 갱신 중 예외 발생")
            tokenProvider.clearTokens()
            return false
        }
    }
}


