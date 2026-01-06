package swyp.team.walkit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber.Tree
import com.google.firebase.crashlytics.FirebaseCrashlytics as Crashlytics
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.core.data.datastore.NidOAuthInitializingCallback
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import swyp.team.walkit.data.remote.billing.BillingManager
import swyp.team.walkit.domain.service.FcmTokenManager
import swyp.team.walkit.worker.SessionSyncWorker
import timber.log.Timber
import javax.inject.Inject

/**
 * 릴리즈 빌드용 Timber Tree - Crashlytics로 로그 전송
 */
class CrashlyticsTree : Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Error와 Warning 레벨 이상만 Crashlytics로 전송
        if (priority >= android.util.Log.WARN) {
            Crashlytics.getInstance().log("$tag: $message")
            t?.let { Crashlytics.getInstance().recordException(it) }
        }
    }
}

@HiltAndroidApp
class WalkingBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Timber 초기화 - 빌드 타입에 따라 다른 트리 사용
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // 릴리즈에서는 Crashlytics로 로그 전송
            Timber.plant(CrashlyticsTree())
        }

        // ✅ 안전하게 BuildConfig에서 API 키 가져오기
        val kakaoAppKey = BuildConfig.KAKAO_APP_KEY
        if (kakaoAppKey.isBlank()) {
            Timber.e("Kakao App Key가 설정되지 않았습니다. local.properties에 KAKAO_APP_KEY를 추가하세요.")
            // TODO: 사용자에게 알림 표시 또는 기능 제한
            // return // 위험: 앱 초기화 완전 중단은 피하자
        } else {
            // Kakao SDK 초기화
            Timber.e("Kakao App Key가 설정 local.properties에 ${kakaoAppKey.take(7)}")
            KakaoSdk.init(this, kakaoAppKey)
            // KakaoMap SDK 초기화
            KakaoMapSdk.init(this, kakaoAppKey)
        }

        // Naver OAuth SDK 초기화 - BuildConfig에서 가져오기
        val naverClientId = BuildConfig.NAVER_CLIENT_ID
        val naverClientSecret = BuildConfig.NAVER_CLIENT_SECRET
        val naverClientName = "walkit"

        if (naverClientId.isBlank() || naverClientSecret.isBlank()) {
            Timber.e("❌ Naver Client 정보가 설정되지 않았습니다. NAVER_CLIENT_ID: '$naverClientId', NAVER_CLIENT_SECRET: '${naverClientSecret.take(5)}***'")
            // TODO: 네이버 로그인을 사용할 수 없음을 사용자에게 알림
        } else {
            Timber.d("🔄 Naver OAuth 초기화 시도 - ClientId: ${naverClientId.take(5)}***")
            try {
                NidOAuth.initialize(
                    this,
                    naverClientId,
                    naverClientSecret,
                    naverClientName,
                    object : NidOAuthInitializingCallback {
                        override fun onSuccess() {
                            Timber.d("✅ Naver OAuth SDK 초기화 성공")
                        }

                        override fun onFailure(e: Exception) {
                            Timber.e(e, "❌ Naver OAuth SDK 초기화 실패: ${e.message}")
                            e.printStackTrace()
                        }
                    },
                )
            } catch (t: Throwable) {
                Timber.e(t, "❌ Naver OAuth 초기화 중 예외 발생: ${t.message}")
                t.printStackTrace()
            }
        }

        // Google Play Billing 초기화
        // Hilt가 완전히 초기화된 후에 주입받아야 하므로 EntryPoint 사용
        val entryPoint = EntryPoints.get(this, BillingEntryPoint::class.java)
        val billingManager = entryPoint.billingManager()
        billingManager.initialize()
        Timber.d("Google Play Billing 초기화 완료")

        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        // Firebase Crashlytics 초기화 - 릴리즈 모드에서만 활성화
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        Timber.d("Firebase 초기화 완료 - Crashlytics: ${!BuildConfig.DEBUG}")

        // NotificationChannel 생성
        createNotificationChannel()

        // FCM 토큰 초기화 및 로그 출력 (릴리즈 빌드에서는 비활성화)
        if (BuildConfig.DEBUG) {
            // FCM 서비스가 완전히 초기화될 때까지 대기
            val fcmEntryPoint = EntryPoints.get(this, FcmEntryPoint::class.java)
            val fcmTokenManager = fcmEntryPoint.fcmTokenManager()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                // FCM 초기화 완료를 위해 짧은 지연 추가
                kotlinx.coroutines.delay(1000)
                // 앱 실행 시마다 현재 토큰 로그 출력
                fcmTokenManager.logCurrentToken()
                // 토큰이 없으면 초기화
                fcmTokenManager.initializeToken()
            }
        }

        // 세션 동기화 WorkManager 초기화
        // 앱 시작 시 주기적 동기화 작업 예약 (30분 간격)
        SessionSyncWorker.schedulePeriodicSync(this, 30L)

        Timber.d("WalkingBuddyApplication onCreate")
    }

    /**
     * NotificationChannel 생성 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "walkit_notification_channel",
                "알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "앱 알림"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Timber.d("NotificationChannel 생성 완료")
        }
    }

    /**
     * BillingManager를 주입받기 위한 EntryPoint
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BillingEntryPoint {
        fun billingManager(): BillingManager
    }

    /**
     * FcmTokenManager를 주입받기 위한 EntryPoint
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FcmEntryPoint {
        fun fcmTokenManager(): FcmTokenManager
    }
}
