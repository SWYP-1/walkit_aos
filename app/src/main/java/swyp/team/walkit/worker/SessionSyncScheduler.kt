package swyp.team.walkit.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import timber.log.Timber
import java.util.concurrent.TimeUnit

object SessionSyncScheduler {

    private const val WORK_NAME = "session_sync_periodic"

    fun schedule(context: Context) {
        // 세션 업로드 작업만 주기적으로 실행 (15분마다)
        val syncRequest = PeriodicWorkRequestBuilder<SessionSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        Timber.d("📌 SessionSyncWorker scheduled (15분마다)")
    }

    /** 세션 업로드만 즉시 실행 */
    fun runSyncOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<SessionSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
