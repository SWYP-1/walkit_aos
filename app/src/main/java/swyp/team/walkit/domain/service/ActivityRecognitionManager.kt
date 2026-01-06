package swyp.team.walkit.domain.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 활동 상태 타입
 */
@Parcelize
@Serializable
enum class ActivityType : Parcelable {
    WALKING, // 걷기
    RUNNING, // 달리기
    IN_VEHICLE, // 차량
    ON_BICYCLE, // 자전거
    STILL, // 정지
    ON_FOOT, // 도보
    UNKNOWN, // 알 수 없음
}

/**
 * 활동 상태 정보
 */
@Parcelize
data class ActivityState(
    val type: ActivityType,
    val confidence: Int, // 신뢰도 (0-100)
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable

/**
 * Activity Recognition을 관리하는 클래스
 *
 * Google Play Services의 Activity Recognition API를 사용하여
 * 사용자의 활동 상태(걷기, 달리기, 차량, 정지 등)를 감지합니다.
 */
@Singleton
class ActivityRecognitionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val activityRecognitionClient: ActivityRecognitionClient =
            ActivityRecognition.getClient(context)

        private var isTracking = false
        private val _currentActivityState = MutableStateFlow<ActivityState?>(null)
        val currentActivityState: StateFlow<ActivityState?> = _currentActivityState.asStateFlow()
        private var activityReceiver: BroadcastReceiver? = null
        private var pendingIntent: PendingIntent? = null

        companion object {
            private const val DETECTION_INTERVAL_MS = 1000L // 1초마다 업데이트 (P0: 즉각 피드백을 위해 단축)
            private const val REQUEST_CODE = 1001
            private const val ACTION_ACTIVITY_UPDATES = "com.google.android.gms.location.ACTIVITY_UPDATES"
        }

        /**
         * Activity Recognition 사용 가능 여부 확인
         */

        /**
         * Activity Recognition 사용 가능 여부 확인
         */
        fun isActivityRecognitionAvailable(): Boolean =
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.content.pm.PackageManager.PERMISSION_GRANTED ==
                        context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    true // Android Q 이하는 권한이 필요 없음
                }
            } catch (t: Throwable) {
                Timber.e(t, "Activity Recognition 권한 확인 실패")
                false
            }

        /**
         * 활동 상태 추적 시작
         */
        fun startTracking() {
            if (isTracking) {
                Timber.d("이미 활동 상태 추적 중입니다")
                return
            }

            if (!isActivityRecognitionAvailable()) {
                Timber.w("Activity Recognition 권한이 없습니다")
                return
            }

            isTracking = true
            registerActivityReceiver()
            requestActivityUpdates()
            Timber.d("활동 상태 추적 시작")
        }

        /**
         * 활동 상태 추적 중지
         */
        fun stopTracking() {
            if (!isTracking) {
                return
            }

            isTracking = false
            removeActivityUpdates()
            unregisterActivityReceiver()
            _currentActivityState.value = null
            Timber.d("활동 상태 추적 중지")
        }

        /**
         * 활동 상태 업데이트를 Flow로 제공
         */
        fun getActivityUpdates(): Flow<ActivityState> =
            callbackFlow {
                val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(
                            context: Context?,
                            intent: Intent?,
                        ) {
                            if (intent?.action != ACTION_ACTIVITY_UPDATES) {
                                return
                            }

                            if (ActivityRecognitionResult.hasResult(intent)) {
                                val result = ActivityRecognitionResult.extractResult(intent)
                                val mostProbableActivity = result?.mostProbableActivity

                                mostProbableActivity?.let { activity ->
                                    val activityType = convertToActivityType(activity.type)
                                    val activityState =
                                        ActivityState(
                                            type = activityType,
                                            confidence = activity.confidence,
                                        )

                                    _currentActivityState.value = activityState
                                    trySend(activityState)
                                    Timber.d("활동 상태 업데이트: ${activityType.name}, 신뢰도: ${activity.confidence}%")
                                }
                            }
                        }
                    }

                val filter = IntentFilter(ACTION_ACTIVITY_UPDATES)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(receiver, filter)
                }

                awaitClose {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (t: Throwable) {
                        Timber.e(t, "BroadcastReceiver 해제 실패")
                    }
                }
            }

        /**
         * DetectedActivity를 ActivityType으로 변환
         */
        fun convertToActivityType(detectedActivity: Int): ActivityType =
            when (detectedActivity) {
                DetectedActivity.WALKING -> ActivityType.WALKING
                DetectedActivity.RUNNING -> ActivityType.RUNNING
                DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
                DetectedActivity.ON_BICYCLE -> ActivityType.ON_BICYCLE
                DetectedActivity.STILL -> ActivityType.STILL
                DetectedActivity.ON_FOOT -> ActivityType.ON_FOOT
                else -> ActivityType.UNKNOWN
            }

        /**
         * Activity Recognition 업데이트 요청
         */
        private fun requestActivityUpdates() {
            pendingIntent = createPendingIntent()
            pendingIntent?.let { intent ->
                activityRecognitionClient
                    .requestActivityUpdates(
                        DETECTION_INTERVAL_MS,
                        intent,
                    ).addOnSuccessListener {
                        Timber.d("Activity Recognition 업데이트 요청 성공")
                    }.addOnFailureListener { e ->
                        Timber.e(e, "Activity Recognition 업데이트 요청 실패")
                    }
            }
        }

        /**
         * Activity Recognition 업데이트 제거
         */
        private fun removeActivityUpdates() {
            pendingIntent?.let { intent ->
                activityRecognitionClient
                    .removeActivityUpdates(intent)
                    .addOnSuccessListener {
                        Timber.d("Activity Recognition 업데이트 제거 성공")
                    }.addOnFailureListener { e ->
                        Timber.e(e, "Activity Recognition 업데이트 제거 실패")
                    }
            }
            pendingIntent = null
        }

        /**
         * BroadcastReceiver 등록
         */
        private fun registerActivityReceiver() {
            // getActivityUpdates()에서 직접 처리하므로 여기서는 등록하지 않음
            // Activity Recognition API가 자동으로 Broadcast를 전송함
        }

        /**
         * BroadcastReceiver 해제
         */
        private fun unregisterActivityReceiver() {
            activityReceiver?.let {
                try {
                    context.unregisterReceiver(it)
                } catch (t: Throwable) {
                    Timber.e(t, "BroadcastReceiver 해제 실패")
                }
            }
            activityReceiver = null
        }

        /**
         * PendingIntent 생성 (BroadcastReceiver용)
         *
         * Activity Recognition API는 이 PendingIntent를 통해 Broadcast를 전송합니다.
         * getActivityUpdates()에서 등록한 BroadcastReceiver가 이를 수신합니다.
         */
        private fun createPendingIntent(): PendingIntent {
            // Activity Recognition API가 전송하는 Broadcast의 action
            val intent =
                Intent(ACTION_ACTIVITY_UPDATES).apply {
                    setPackage(context.packageName)
                }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
