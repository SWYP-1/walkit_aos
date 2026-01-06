package swyp.team.walkit.domain.service

import android.content.Context
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import swyp.team.walkit.data.local.datastore.FcmTokenDataStore
import swyp.team.walkit.data.remote.auth.TokenProvider
import swyp.team.walkit.data.repository.NotificationRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 토큰 관리 서비스
 *
 * FCM 토큰 발급, 로컬 저장, 서버 동기화를 담당합니다.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val fcmTokenDataStore: FcmTokenDataStore,
    private val notificationRepository: NotificationRepository,
    private val tokenProvider: TokenProvider,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 앱 실행 시 FCM 토큰 확인 및 로그 출력
     * 저장된 토큰이 있으면 출력하고, 없으면 새로 발급
     * 
     * 주의: 서버 전송은 하지 않습니다. 서버 전송은 onNewToken에서만 처리됩니다.
     */
    suspend fun logCurrentToken() {
        try {
            Timber.d("=== FCM 토큰 상태 확인 시작 ===")

            // 저장된 토큰 확인
            val savedToken = fcmTokenDataStore.getToken()
            if (savedToken != null) {
                Timber.d("FCM 토큰 (저장된 토큰): ${savedToken.take(50)}...") // 보안상 앞부분만 출력
            } else {
                Timber.d("FCM 토큰: 저장된 토큰 없음, 새로 발급 시도")
            }

            // Firebase에서 최신 토큰 가져오기
            val currentToken = firebaseMessaging.token.await()
            Timber.d("FCM 토큰 (현재 Firebase 토큰): ${currentToken.take(50)}...")

            // 토큰 비교
            val tokensMatch = savedToken == currentToken
            Timber.d("토큰 일치 여부: $tokensMatch")

            // 저장된 토큰과 다르면 로컬에만 업데이트 (서버 전송은 onNewToken에서 처리)
            if (!tokensMatch) {
                Timber.d("FCM 토큰 변경 감지, 로컬 업데이트 진행")
                fcmTokenDataStore.saveToken(currentToken)
                Timber.d("토큰 로컬 저장 완료")

                // 로그인 상태라면 서버에도 업데이트
                if (isLoggedIn()) {
                    Timber.d("로그인 상태이므로 변경된 토큰을 서버로 전송")
                    syncTokenToServer(currentToken)
                }
            } else {
                Timber.d("토큰이 최신 상태입니다")
            }

            Timber.d("=== FCM 토큰 상태 확인 완료 ===")
        } catch (t: Throwable) {
            Timber.e(t, "FCM 토큰 확인 실패")
        }
    }

    /**
     * 앱 최초 실행 시 FCM 토큰 발급 및 로컬 저장
     * 재시도 로직 포함 (최대 3회, 지수 백오프)
     * 
     * 주의: 서버 전송은 하지 않습니다. 서버 전송은 onNewToken에서만 처리됩니다.
     * 로그인 후 서버에 전송하려면 syncTokenToServer()를 별도로 호출하세요.
     */
    suspend fun initializeToken() {
        val maxRetries = 3
        var retryDelay = 1000L // 1초부터 시작

        repeat(maxRetries) { attempt ->
            try {
                val token = firebaseMessaging.token.await()
                Timber.d("FCM 토큰 발급 성공: $token")
                fcmTokenDataStore.saveToken(token)
                // 서버 전송은 onNewToken에서 자동으로 처리되므로 여기서는 하지 않음
                return // 성공 시 종료
            } catch (t: Throwable) {
                val isLastAttempt = attempt == maxRetries - 1
                if (isLastAttempt) {
                    Timber.e(t, "FCM 토큰 발급 실패 (최대 재시도 횟수 도달)")
                } else {
                    Timber.w(t, "FCM 토큰 발급 실패, ${retryDelay}ms 후 재시도 (시도 ${attempt + 1}/$maxRetries)")
                    delay(retryDelay)
                    retryDelay *= 2 // 지수 백오프: 1초 -> 2초 -> 4초
                }
            }
        }
    }

    /**
     * FCM 토큰 갱신 시 호출
     * 로그인 상태라면 즉시 서버에 업데이트, 아니면 로컬에만 저장
     */
    suspend fun refreshToken(newToken: String) {
        try {
            Timber.d("=== FCM 토큰 갱신 시작 ===")
            Timber.d("새 토큰: $newToken")
            Timber.d("로그인 상태: ${isLoggedIn()}")

            fcmTokenDataStore.saveToken(newToken)
            Timber.d("토큰 로컬 저장 완료")

            // 로그인 상태라면 서버에 업데이트
            if (isLoggedIn()) {
                Timber.d("로그인 상태이므로 서버로 토큰 전송 시도")
                syncTokenToServer(newToken)
            } else {
                Timber.d("로그아웃 상태이므로 서버 전송 생략 (로컬에만 저장)")
            }
            Timber.d("=== FCM 토큰 갱신 완료 ===")
        } catch (t: Throwable) {
            Timber.e(t, "FCM 토큰 갱신 처리 실패")
        }
    }

    /**
     * 서버에 FCM 토큰 등록/업데이트
     */
    suspend fun syncTokenToServer(token: String? = null) {
        try {
            Timber.d("=== FCM 토큰 서버 동기화 시작 ===")

            val tokenToSync = token ?: fcmTokenDataStore.fcmToken.first()
            Timber.d("동기화할 토큰: $tokenToSync")

            if (tokenToSync == null) {
                Timber.w("동기화할 FCM 토큰이 없습니다")
                return
            }

            val deviceId = getDeviceId()
            Timber.d("기기 ID: $deviceId")

            Timber.d("서버 API 호출 시작")
            val result = notificationRepository.registerFcmToken(tokenToSync, deviceId)
            Timber.d("서버 API 호출 완료, 결과: ${result::class.simpleName}")

            when (result) {
                is swyp.team.walkit.core.Result.Success -> {
                    Timber.d("✅ FCM 토큰 서버 동기화 성공")
                }
                is swyp.team.walkit.core.Result.Error -> {
                    Timber.e(result.exception, "❌ FCM 토큰 서버 동기화 실패: ${result.message}")
                    Timber.e("에러 상세: ${result.exception?.stackTraceToString()}")
                }
                swyp.team.walkit.core.Result.Loading -> {
                    Timber.d("서버 동기화 로딩 중")
                }
            }
            Timber.d("=== FCM 토큰 서버 동기화 완료 ===")
        } catch (t: Throwable) {
            Timber.e(t, "FCM 토큰 서버 동기화 중 예외 발생")
            Timber.e("예외 상세: ${t.stackTraceToString()}")
        }
    }

    /**
     * 로그인 상태 확인
     */
    private fun isLoggedIn(): Boolean {
        return tokenProvider.getAccessToken() != null
    }

    /**
     * 기기 ID 가져오기 (Android ID 사용)
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: ""
    }
}

