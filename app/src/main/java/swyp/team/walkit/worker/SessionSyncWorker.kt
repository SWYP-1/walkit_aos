package swyp.team.walkit.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import swyp.team.walkit.R
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.domain.repository.WalkRepository
import swyp.team.walkit.core.Result
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 미동기화 WalkingSession들을 주기적으로 서버에 동기화하는 Worker
 *
 * WorkManager를 통해 백그라운드에서 실행되며,
 * PENDING 또는 FAILED 상태의 세션들을 찾아서 서버 동기화를 시도합니다.
 */
@HiltWorker
class SessionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val walkingSessionRepository: WalkingSessionRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Timber.d("🚀 SessionSyncWorker 실행 (로컬 → 서버 업로드)")

        return try {
            // 로컬 미동기화 세션들 서버에 업로드
            walkingSessionRepository.syncAllPendingSessions()
            Timber.d("✅ 로컬 세션 서버 업로드 완료")

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "❌ SessionSyncWorker 실패")
            Result.retry()
        }
    }

    companion object {
        /**
         * 테스트용 알림 생성 함수
         */
        fun createTestNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, "walkit_notification_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("테스트 알림")
                .setContentText("SessionSyncWorker 테스트 알림입니다")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
            Timber.d("🔔 테스트 알림 생성됨")
        }
    }
}