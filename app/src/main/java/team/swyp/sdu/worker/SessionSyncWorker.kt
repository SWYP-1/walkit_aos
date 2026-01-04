package team.swyp.sdu.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import team.swyp.sdu.data.repository.WalkingSessionRepository
import timber.log.Timber
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
    @Assisted workerParams: WorkerParameters,
    private val walkingSessionRepository: WalkingSessionRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("SessionSyncWorker 시작")

            // 미동기화 세션들을 모두 동기화
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

