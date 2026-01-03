package team.swyp.sdu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
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
import team.swyp.sdu.data.remote.billing.BillingManager
import team.swyp.sdu.domain.service.FcmTokenManager
import team.swyp.sdu.worker.SessionSyncWorker
import timber.log.Timber
import java.io.File
import java.util.Properties
import javax.inject.Inject

@HiltAndroidApp
class WalkingBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Timber 초기화
        // 개발 중이므로 항상 DebugTree 사용
        // 릴리즈 빌드에서는 Crashlytics 등으로 로그 전송 가능
        Timber.plant(Timber.DebugTree())

        // ✅ 안전하게 BuildConfig에서 API 키 가져오기
        val kakaoAppKey = BuildConfig.KAKAO_APP_KEY
        if (kakaoAppKey.isBlank()) {
            Timber.e("Kakao App Key가 설정되지 않았습니다. local.properties에 KAKAO_APP_KEY를 추가하세요.")
            return
        }

        // Kakao SDK 초기화
        KakaoSdk.init(this, kakaoAppKey)

        // KakaoMap SDK 초기화
        KakaoMapSdk.init(this, kakaoAppKey)

        // Naver OAuth SDK 초기화 - local.properties에서 직접 읽기
        val localProperties = Properties().apply {
            try {
                val propertiesFile = File(filesDir.parent, "local.properties")
                if (propertiesFile.exists()) {
                    load(propertiesFile.inputStream())
                }
            } catch (e: Exception) {
                Timber.e(e, "local.properties 파일 읽기 실패")
            }
        }
        val naverClientId = localProperties.getProperty("NAVER_CLIENT_ID", "")
        val naverClientSecret = localProperties.getProperty("NAVER_CLIENT_SECRET", "")
        val naverClientName = "walkit"

        if (naverClientId.isBlank() || naverClientSecret.isBlank()) {
            Timber.e("Naver Client 정보가 설정되지 않았습니다. local.properties에 NAVER_CLIENT_ID와 NAVER_CLIENT_SECRET을 추가하세요.")
            return
        }
        NidOAuth.initialize(
            this,
            naverClientId,
            naverClientSecret,
            naverClientName,
            object : NidOAuthInitializingCallback {
                override fun onSuccess() {
                    Timber.d("Naver OAuth SDK 초기화 성공")
                }

                override fun onFailure(e: Exception) {
                    Timber.e(e, "Naver OAuth SDK 초기화 실패")
                }
            },
        )

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

        // FCM 토큰 초기화 및 로그 출력 (비동기, 지연 실행)
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

        // 세션 동기화 WorkManager 초기화
        // 앱 시작 시 주기적 동기화 작업 예약 (15분 간격)
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
