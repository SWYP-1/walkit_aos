package team.swyp.sdu.domain.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import team.swyp.sdu.MainActivity
import team.swyp.sdu.R
import team.swyp.sdu.domain.repository.FriendRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * FCM 메시지 수신 및 토큰 갱신을 처리하는 서비스
 */
@AndroidEntryPoint
class WalkItFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var friendRepository: FriendRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "walkit_notification_channel"
        private const val CHANNEL_NAME = "알림"
        private var notificationIdCounter = 1000 // 동적 ID 시작값
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * FCM 토큰 갱신 시 호출
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM 토큰 갱신: $token")

        serviceScope.launch {
            fcmTokenManager.refreshToken(token)
        }
    }

    /**
     * FCM 메시지 수신 시 호출
     * - notification 메시지: 시스템에서 자동으로 알림 표시 (백그라운드/종료 시)
     * - data 메시지: 앱에서 직접 알림 처리 (모든 상태)
     * - notification + data 혼합: 포그라운드에서 onMessageReceived 처리
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 상세 로깅으로 디버깅
        Timber.d("=== FCM 메시지 수신 시작 ===")
        Timber.d("메시지 ID: ${remoteMessage.messageId}")
        Timber.d("발신자: ${remoteMessage.from}")
        Timber.d("notification 존재: ${remoteMessage.notification != null}")
        Timber.d("data 존재: ${remoteMessage.data.isNotEmpty()}")
        Timber.d("TTL: ${remoteMessage.ttl}")
        Timber.d("전송 시간: ${remoteMessage.sentTime}")


        if (remoteMessage.notification != null) {
            Timber.d("Notification 제목: ${remoteMessage.notification?.title}")
            Timber.d("Notification 본문: ${remoteMessage.notification?.body}")
        }

        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Data 내용: ${remoteMessage.data}")
        }

        Timber.d("=== FCM 메시지 수신 끝 ===")

        // 포그라운드 상태 확인
        val isForeground = isAppInForeground()
        Timber.d("현재 앱 상태: ${if (isForeground) "FOREGROUND" else "BACKGROUND"}")

        // ✅ 현재 구현: notification이든 data든 모두 처리 가능
        Timber.d("메시지 처리 시작")

        // 2. data-only 메시지인 경우 직접 알림 생성 (서버가 잘못된 형식으로 줘도 처리 가능)
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("✅ Data-only 메시지 처리 (서버가 notification 안 줘도 OK)")
            showCustomNotification(remoteMessage.data)
        } else {
            Timber.w("❌ FCM 메시지에 notification과 data가 모두 없음 - 빈 메시지")
        }
    }

    /**
     * 시스템 notification 메시지 표시 (notification 필드가 있는 경우)
     */
    private fun showSystemNotification(
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 데이터 전달 (필요시)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // 동적 알림 ID 생성 (여러 알림이 동시에 표시되도록)
        val notificationId = synchronized(this) {
            notificationIdCounter++
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: 알림 아이콘으로 변경
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 포그라운드에서도 표시되도록 MAX 우선순위
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 메시지 카테고리
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금화면에서도 표시
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true) // 시간 표시
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리, 진동 등 모든 기본값 사용
            .build()

        // NotificationManagerCompat를 사용하여 알림 표시
        val notificationManager = NotificationManagerCompat.from(this)

        // 포그라운드 상태 확인
        val isAppInForeground = isAppInForeground()
        Timber.d("앱 포그라운드 상태: $isAppInForeground")

        // 알림 권한 확인
        if (!notificationManager.areNotificationsEnabled()) {
            Timber.w("알림 권한이 비활성화되어 있어 알림을 표시할 수 없습니다")
            return
        }

        // 채널 차단 여부 확인 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                Timber.w("알림 채널이 차단되어 있어 알림을 표시할 수 없습니다")
                return
            }
        }

        try {
            notificationManager.notify(notificationId, notification)
            Timber.d("알림 표시 완료: id=$notificationId, title=$title, foreground=$isAppInForeground")
        } catch (e: SecurityException) {
            Timber.e(e, "알림 표시 중 SecurityException 발생")
        } catch (t: Throwable) {
            Timber.e(t, "알림 표시 중 예외 발생")
        }
    }

    /**
     * 커스텀 알림 표시 (data-only 메시지용)
     * 서버에서 제공하는 title과 body를 그대로 사용
     */
    private fun showCustomNotification(data: Map<String, String>) {
        val typeString = data["type"] ?: "GENERAL"

        // FRIEND_UPDATED 같은 특수 케이스 처리 (silent push)
        if (typeString.uppercase() == "FRIEND_UPDATED") {
            Timber.d("친구 상태 변경 이벤트 수신 - 캐시 무효화 및 이벤트 발행")
            serviceScope.launch {
                friendRepository.invalidateCache()
                friendRepository.emitFriendUpdated()
            }
            return // silent push는 알림 표시하지 않음
        }

        // 서버에서 제공하는 title과 body를 그대로 사용
        val title = data["title"] ?: "알림"
        val body = data["body"] ?: "새로운 알림이 있습니다"

        Timber.d("커스텀 알림 생성: type=$typeString, title=$title, body=$body")

        showSystemNotification(title, body, data)
    }

    /**
     * 앱이 포그라운드에 있는지 확인
     */
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        val packageName = packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * NotificationChannel 생성 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "앱 알림"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

