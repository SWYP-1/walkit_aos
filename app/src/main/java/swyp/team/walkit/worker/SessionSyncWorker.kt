package swyp.team.walkit.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import swyp.team.walkit.R
import swyp.team.walkit.data.repository.WalkingSessionRepository
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
class SessionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    val walkingSessionRepository: WalkingSessionRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("SessionSyncWorker 시작")

            // 테스트용 노티피케이션 생성
//            showTestNotification()

            // 미동기화 세션들을 모두 동기화
            // TODO: Repository 주입 방식으로 변경 필요
            walkingSessionRepository.syncAllPendingSessions()

            Timber.d("SessionSyncWorker 완료")
            Result.success()

        } catch (t: Throwable) {
            Timber.e(t, "SessionSyncWorker 실패")
            // 실패 시 재시도 (최대 3회)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * 테스트용 노티피케이션 표시 (WorkManager 실행 확인용)
     */
    private fun showTestNotification() {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val notification = NotificationCompat.Builder(applicationContext, "walkit_notification_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("세션 동기화 작업 실행됨")
                .setContentText("SessionSyncWorker가 ${currentTime}에 실행되었습니다")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("SessionSyncWorker 테스트 노티피케이션\n실행 시간: ${currentTime}\nWorkManager가 정상 작동 중입니다!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            // 고유한 notification ID 생성 (중복 방지)
            val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1000
            notificationManager.notify(notificationId, notification)

            Timber.d("SessionSyncWorker 테스트 노티피케이션 표시됨: $currentTime")

        } catch (t: Throwable) {
            Timber.e(t, "노티피케이션 표시 실패")
        }
    }

    companion object {
        private const val WORK_NAME = "session_sync_work"

        /**
         * 주기적 세션 동기화 작업 예약
         *
         * @param context Application Context
         * @param intervalMinutes 동기화 간격 (분 단위, 기본 15분)
         */
        fun schedulePeriodicSync(
            context: Context,
            intervalMinutes: Long = 15L
        ) {
            val workRequest = PeriodicWorkRequestBuilder<SessionSyncWorker>(
                repeatInterval = intervalMinutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // 네트워크 연결 필요
                    .setRequiresBatteryNotLow(true) // 배터리 부족 시 실행 안 함
                    .build()
            )
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // 이미 실행중이면 유지
                workRequest
            )

            Timber.d("SessionSyncWorker 주기적 작업 예약됨: ${intervalMinutes}분 간격")
        }

        /**
         * 즉시 세션 동기화 작업 실행 (한 번만)
         *
         * @param context Application Context
         */
        fun scheduleOneTimeSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SessionSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Timber.d("SessionSyncWorker 즉시 작업 실행됨")
        }

        /**
         * 테스트용 즉시 작업 실행 (네트워크 제약 없음)
         *
         * @param context Application Context
         */
        fun scheduleTestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SessionSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // 네트워크 제약 없음
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Timber.d("SessionSyncWorker 테스트 작업 즉시 실행됨")
        }

        /**
         * 세션 동기화 작업 취소
         *
         * @param context Application Context
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("SessionSyncWorker 작업 취소됨")
        }
    }
}

