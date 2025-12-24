package team.swyp.sdu.domain.service

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
import team.swyp.sdu.data.local.datastore.FcmTokenDataStore
import team.swyp.sdu.data.remote.auth.TokenProvider
import team.swyp.sdu.data.repository.NotificationRepository
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
            // 저장된 토큰 확인
            val savedToken = fcmTokenDataStore.getToken()
            if (savedToken != null) {
                Timber.d("FCM 토큰 (저장된 토큰): $savedToken")
            } else {
                Timber.d("FCM 토큰: 저장된 토큰 없음, 새로 발급 시도")
            }
            
            // Firebase에서 최신 토큰 가져오기
            val currentToken = firebaseMessaging.token.await()
            Timber.d("FCM 토큰 (현재 토큰): $currentToken")
            
            // 저장된 토큰과 다르면 로컬에만 업데이트 (서버 전송은 onNewToken에서 처리)
            if (savedToken != currentToken) {
                Timber.d("FCM 토큰 변경 감지, 로컬 업데이트: $savedToken -> $currentToken")
                fcmTokenDataStore.saveToken(currentToken)
                // 서버 전송은 onNewToken에서 자동으로 처리되므로 여기서는 하지 않음
            }
        } catch (e: Exception) {
            Timber.e(e, "FCM 토큰 확인 실패")
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
            } catch (e: Exception) {
                val isLastAttempt = attempt == maxRetries - 1
                if (isLastAttempt) {
                    Timber.e(e, "FCM 토큰 발급 실패 (최대 재시도 횟수 도달)")
                } else {
                    Timber.w(e, "FCM 토큰 발급 실패, ${retryDelay}ms 후 재시도 (시도 ${attempt + 1}/$maxRetries)")
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
            Timber.d("FCM 토큰 갱신: $newToken")
            fcmTokenDataStore.saveToken(newToken)

            // 로그인 상태라면 서버에 업데이트
            if (isLoggedIn()) {
                syncTokenToServer(newToken)
            }
        } catch (e: Exception) {
            Timber.e(e, "FCM 토큰 갱신 처리 실패")
        }
    }

    /**
     * 서버에 FCM 토큰 등록/업데이트
     */
    suspend fun syncTokenToServer(token: String? = null) {
        try {
            val tokenToSync = token ?: fcmTokenDataStore.fcmToken.first()
            if (tokenToSync == null) {
                Timber.w("동기화할 FCM 토큰이 없습니다")
                return
            }

            val deviceId = getDeviceId()
            val result = notificationRepository.registerFcmToken(tokenToSync, deviceId)

            when (result) {
                is team.swyp.sdu.core.Result.Success -> {
                    Timber.d("FCM 토큰 서버 동기화 성공")
                }
                is team.swyp.sdu.core.Result.Error -> {
                    Timber.e(result.exception, "FCM 토큰 서버 동기화 실패: ${result.message}")
                }
                team.swyp.sdu.core.Result.Loading -> {
                    // 처리 불필요
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FCM 토큰 서버 동기화 실패")
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

