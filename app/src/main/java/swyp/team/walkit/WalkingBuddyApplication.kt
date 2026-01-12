package swyp.team.walkit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.core.data.datastore.NidOAuthInitializingCallback
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import swyp.team.walkit.core.Result
import swyp.team.walkit.worker.SessionSyncScheduler
import timber.log.Timber
import timber.log.Timber.Tree
import javax.inject.Inject

/* -------------------- Crashlytics Timber Tree -------------------- */

class CrashlyticsTree : Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= android.util.Log.WARN) {
            FirebaseCrashlytics.getInstance().log("$tag: $message")
            t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }
}

/* -------------------- Application -------------------- */

@HiltAndroidApp
class WalkingBuddyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        initTimber()
        initFirebase()
        initSdk()
        createNotificationChannel()

        // 주기적 세션 동기화 스케줄링 (백그라운드)
        SessionSyncScheduler.schedule(this)

        Timber.d("✅ WalkingBuddyApplication initialized")
    }

    /* -------------------- WorkManager -------------------- */

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG
                else android.util.Log.INFO
            )
            .build()

    /* -------------------- Init Methods -------------------- */

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun initFirebase() {
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun initSdk() {
        initKakao()
        initNaver()
        initBilling()
    }

    private fun initKakao() {
        val kakaoKey = BuildConfig.KAKAO_APP_KEY
        if (kakaoKey.isBlank()) {
            Timber.e("❌ Kakao App Key missing")
            return
        }
        KakaoSdk.init(this, kakaoKey)
        KakaoMapSdk.init(this, kakaoKey)
        Timber.d("✅ Kakao SDK initialized")
    }

    private fun initNaver() {
        val clientId = BuildConfig.NAVER_CLIENT_ID
        val clientSecret = BuildConfig.NAVER_CLIENT_SECRET

        if (clientId.isBlank() || clientSecret.isBlank()) {
            Timber.e("❌ Naver Client info missing")
            return
        }

        NidOAuth.initialize(
            this,
            clientId,
            clientSecret,
            "walkit",
            object : NidOAuthInitializingCallback {
                override fun onSuccess() {
                    Timber.d("✅ Naver OAuth initialized")
                }

                override fun onFailure(e: Exception) {
                    Timber.e(e, "❌ Naver OAuth init failed")
                }
            }
        )
    }

    private fun initBilling() {
        val entryPoint = EntryPoints.get(this, BillingEntryPoint::class.java)
        entryPoint.billingManager().initialize()
        Timber.d("✅ Billing initialized")
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Walkit 알림"
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        Timber.d("✅ NotificationChannel created")
    }

    /* -------------------- EntryPoints -------------------- */

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BillingEntryPoint {
        fun billingManager(): swyp.team.walkit.data.remote.billing.BillingManager
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "walkit_notification_channel"
    }
}
