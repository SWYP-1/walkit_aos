package swyp.team.walkit.data.remote.interceptor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import swyp.team.walkit.core.AuthEventBus
import swyp.team.walkit.data.api.auth.AuthApi
import swyp.team.walkit.data.remote.auth.TokenProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 401 Unauthorized 응답 시 토큰 갱신을 처리하는 Authenticator
 *
 * ⚠️ DEPRECATED: AuthInterceptor가 401을 처리하므로 더 이상 사용되지 않음
 * 서버가 WWW-Authenticate 헤더를 보내지 않아 실제로는 호출되지 않음
 *
 * 이 클래스는 유지되지만 실제로는 실행되지 않음
 * AuthInterceptor가 모든 401 처리를 담당하므로 이 클래스의 refreshTokensIfNeeded 호출도
 * 새로운 refreshTokensOn401으로 변경됨 (호환성 유지)
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val context: Context,
    private val tokenProvider: TokenProvider,
    private val authEventBus: AuthEventBus,
    @Named("walkit") private val retrofitProvider: Provider<Retrofit>,
) : Authenticator {

    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 🔒 TokenAuthenticator 레벨 동시성 제어
    // 여러 401 요청이 동시에 도착해도 하나의 refresh만 수행
    private val authenticatorMutex = kotlinx.coroutines.sync.Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 401이 아니면 처리하지 않음
        if (response.code != 401) {
            return null
        }

        // 무한 루프 방지: 이미 재시도한 요청이면 중단
        if (isRetryAttempt(response)) {
            Timber.w("TokenAuthenticator - 이미 재시도한 요청(${response.request.url}), 로그인 필요")
            notifyRequireLogin()
            return null
        }

        Timber.d("TokenAuthenticator - 401 감지(${response.request.url.encodedPath}), 토큰 갱신 시도")

        // 🔒 여러 401 요청 동시 도착 시 하나의 refresh만 수행
        return runBlocking {
            authenticatorMutex.withLock {
            try {
                val retrofit = retrofitProvider.get()
                val authApi = retrofit.create(AuthApi::class.java)
                val refreshToken = tokenProvider.getRefreshToken()

                if (refreshToken.isNullOrBlank()) {
                    Timber.w("TokenAuthenticator - 리프레시 토큰 없음")
                    notifyRequireLogin()
                    return@runBlocking null
                }

                // ⚠️ 중요: 이미 다른 요청에서 토큰이 갱신되었는지 확인
                // 첫 번째 refresh 성공 후 캐시된 토큰이 있으면 재사용
                val currentToken = tokenProvider.getAccessToken()
                if (!currentToken.isNullOrBlank()) {
                    Timber.d("TokenAuthenticator - 이미 유효한 토큰 존재, 재사용")
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // TokenProvider를 통한 토큰 갱신 (앱 전체 단일 refresh 보장)
                val refreshSuccess = tokenProvider.refreshTokensOn401(authApi)

                if (refreshSuccess) {
                    val newAccessToken = tokenProvider.getAccessToken()
                    if (!newAccessToken.isNullOrBlank()) {
                        Timber.i("TokenAuthenticator - 토큰 갱신 성공, 재시도")
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        Timber.e("TokenAuthenticator - 갱신 후 토큰 없음")
                    }
                } else {
                    Timber.e("TokenAuthenticator - 토큰 갱신 실패")
                }

                // 갱신 실패 시 로그인 필요
                notifyRequireLogin()
                null

            } catch (e: Exception) {
                Timber.e(e, "TokenAuthenticator - 토큰 갱신 예외")
                notifyRequireLogin()
                null
            }
            } // authenticatorMutex.withLock 끝
        }
    }

    /**
     * 재시도 요청인지 확인 (무한 루프 방지)
     * 동일 요청에 대해 2회 이상 401이 발생한 경우에만 재시도로 간주
     */
    private fun isRetryAttempt(response: Response): Boolean {
        var count = 0
        var current: Response? = response.priorResponse

        while (current != null) {
            count++
            current = current.priorResponse
        }

        // 2회 이상 재시도한 경우에만 로그인 이벤트 발생
        return count >= 2
    }

    /**
     * 로그인 필요 이벤트 발생
     */
    private fun notifyRequireLogin() {
        eventScope.launch {
            try {
                authEventBus.notifyRequireLogin()
                Timber.d("TokenAuthenticator - 로그인 필요 이벤트 발생")
            } catch (e: Exception) {
                Timber.e(e, "TokenAuthenticator - 로그인 이벤트 실패")
            }
        }
    }
}










