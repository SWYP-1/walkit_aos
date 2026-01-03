package team.swyp.sdu.domain.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 움직임 상태 (가속도계 기반)
 */
enum class MovementState {
    STILL, // 정지
    WALKING, // 걷기
    RUNNING, // 달리기
    UNKNOWN, // 알 수 없음
}

/**
 * 가속도계 기반 움직임 감지 결과
 */
data class MovementDetection(
    val state: MovementState,
    val acceleration: Float, // 총 가속도 (m/s²)
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * 가속도계를 사용하여 즉각적인 움직임 감지를 제공하는 클래스
 *
 * Activity Recognition API (1초 간격)보다 빠른 피드백을 제공합니다.
 * 가속도계는 실시간으로 업데이트되므로 움직임을 즉시 감지할 수 있습니다.
 */
@Singleton
class AccelerometerManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sensorManager: SensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        private val accelerometerSensor: Sensor? =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        private var isTracking = false

        // 움직임 감지 임계값 (m/s²) - 중력 제거 후 변화량 기준
        companion object {
            private const val STILL_THRESHOLD = 1.0f // 정지: 1.0 m/s² 이하
            private const val WALKING_THRESHOLD = 2.5f // 걷기: 1.0 ~ 2.5 m/s²
            private const val RUNNING_THRESHOLD = 4.0f // 달리기: 2.5 ~ 4.0 m/s²
            // 4.0 m/s² 이상은 RUNNING으로 분류

            // 히스테리시스 패턴을 위한 추가 임계값 (상태 전환 시 노이즈 방지)
            // 달리기 감지를 더 엄격하게 하기 위해 임계값 증가
            private const val WALKING_TO_RUNNING_THRESHOLD = 4.5f // 걷기 -> 달리기: 4.5 m/s² 필요 (3.5 -> 4.5로 증가)
            private const val RUNNING_TO_WALKING_THRESHOLD = 2.5f // 달리기 -> 걷기: 2.5 m/s² 이하로 떨어져야 (2.0 -> 2.5로 증가)

            // 이동 평균을 위한 윈도우 크기 (더 많은 샘플로 평활화)
            private const val MOVING_AVERAGE_WINDOW = 10 // 5 -> 10으로 증가하여 더 안정적인 판단
        }

        /**
         * 가속도계 센서 사용 가능 여부 확인
         */
        fun isAccelerometerAvailable(): Boolean = accelerometerSensor != null

        /**
         * 움직임 감지 시작
         */
        fun startTracking() {
            if (isTracking) {
                Timber.d("이미 가속도계 추적 중입니다")
                return
            }

            if (!isAccelerometerAvailable()) {
                Timber.e("가속도계 센서를 사용할 수 없습니다")
                return
            }

            isTracking = true
            Timber.d("가속도계 추적 시작")
        }

        /**
         * 움직임 감지 중지
         */
        fun stopTracking() {
            if (!isTracking) {
                return
            }

            isTracking = false
            Timber.d("가속도계 추적 중지")
        }

        /**
         * 움직임 감지 업데이트를 Flow로 제공
         * 가속도계 센서를 실시간으로 모니터링하여 움직임 상태를 감지합니다.
         */
        fun getMovementUpdates(): Flow<MovementDetection> =
            callbackFlow {
                if (!isAccelerometerAvailable()) {
                    Timber.e("가속도계 센서를 사용할 수 없습니다")
                    close()
                    return@callbackFlow
                }

                if (!isTracking) {
                    Timber.w("추적 중이 아니므로 가속도계 이벤트 무시")
                    close()
                    return@callbackFlow
                }

                // 이동 평균을 위한 버퍼
                val accelerationBuffer = mutableListOf<Float>()
                // 중력 가속도 추정을 위한 버퍼 (초기 10개 샘플 평균)
                val gravityBuffer = mutableListOf<Float>()
                var gravityEstimate: Float? = null
                var sampleCount = 0

                // 히스테리시스 패턴을 위한 이전 상태 추적
                var previousState: MovementState? = null

                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent?) {
                            if (!isTracking) {
                                return
                            }

                            event?.let {
                                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                                    val x = it.values[0]
                                    val y = it.values[1]
                                    val z = it.values[2]

                                    // 총 가속도 크기 계산 (중력 포함)
                                    val totalAcceleration = sqrt(x * x + y * y + z * z)

                                    // 초기 10개 샘플로 중력 가속도 추정 (약 9.8 m/s²)
                                    if (sampleCount < 10) {
                                        gravityBuffer.add(totalAcceleration)
                                        sampleCount++
                                        if (sampleCount == 10) {
                                            gravityEstimate = gravityBuffer.average().toFloat()
                                            Timber.d("중력 가속도 추정 완료: $gravityEstimate m/s²")
                                        }
                                        return // 초기 샘플링 중에는 움직임 감지하지 않음
                                    }

                                    // 중력 가속도 제거 (움직임만 감지)
                                    val movementAcceleration =
                                        if (gravityEstimate != null) {
                                            abs(totalAcceleration - gravityEstimate!!)
                                        } else {
                                            // 중력 추정이 아직 안 된 경우 기본값 사용
                                            abs(totalAcceleration - 9.8f)
                                        }

                                    // 이동 평균 적용 (노이즈 필터링)
                                    accelerationBuffer.add(movementAcceleration)
                                    if (accelerationBuffer.size > MOVING_AVERAGE_WINDOW) {
                                        accelerationBuffer.removeAt(0)
                                    }

                                    val averageAcceleration = accelerationBuffer.average().toFloat()

                                    // 움직임 상태 판단 (히스테리시스 패턴 적용)
                                    val state =
                                        when (previousState) {
                                            MovementState.RUNNING -> {
                                                // 달리기 중이었으면, 더 낮은 임계값으로 떨어져야 걷기로 전환
                                                when {
                                                    averageAcceleration <= STILL_THRESHOLD -> MovementState.STILL
                                                    averageAcceleration <= RUNNING_TO_WALKING_THRESHOLD -> MovementState.WALKING
                                                    else -> MovementState.RUNNING // 달리기 유지
                                                }
                                            }

                                            MovementState.WALKING -> {
                                                // 걷기 중이었으면, 더 높은 임계값을 넘어야 달리기로 전환
                                                when {
                                                    averageAcceleration <= STILL_THRESHOLD -> MovementState.STILL
                                                    averageAcceleration >= WALKING_TO_RUNNING_THRESHOLD -> MovementState.RUNNING
                                                    else -> MovementState.WALKING // 걷기 유지
                                                }
                                            }

                                            else -> {
                                                // 정지 또는 초기 상태: 일반 임계값 사용
                                                when {
                                                    averageAcceleration <= STILL_THRESHOLD -> MovementState.STILL
                                                    averageAcceleration <= WALKING_THRESHOLD -> MovementState.WALKING
                                                    averageAcceleration <= RUNNING_THRESHOLD -> MovementState.RUNNING
                                                    else -> MovementState.RUNNING
                                                }
                                            }
                                        }

                                    // 상태 업데이트
                                    previousState = state

                                    // 움직임 감지 결과 전송
                                    val detection =
                                        MovementDetection(
                                            state = state,
                                            acceleration = averageAcceleration,
                                            timestamp = System.currentTimeMillis(),
                                        )

                                    trySend(detection)
                                }
                            }
                        }

                        override fun onAccuracyChanged(
                            sensor: Sensor?,
                            accuracy: Int,
                        ) {
                            when (accuracy) {
                                SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                                    Timber.w("가속도계 센서 정확도가 신뢰할 수 없습니다")
                                }

                                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                                    Timber.w("가속도계 센서 정확도가 낮습니다")
                                }

                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                                    Timber.d("가속도계 센서 정확도: 보통")
                                }

                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                                    Timber.d("가속도계 센서 정확도: 높음")
                                }
                            }
                        }
                    }

                // 가속도계 센서 등록 (게임용 속도 - 50Hz)
                val success =
                    sensorManager.registerListener(
                        listener,
                        accelerometerSensor,
                        SensorManager.SENSOR_DELAY_GAME, // 게임용 속도 (50Hz)
                    )

                if (success) {
                    Timber.d("가속도계 센서 등록 성공")
                } else {
                    Timber.e("가속도계 센서 등록 실패")
                    close()
                    return@callbackFlow
                }

                awaitClose {
                    try {
                        sensorManager.unregisterListener(listener)
                        Timber.d("가속도계 센서 리스너 해제")
                    } catch (e: Exception) {
                        Timber.e(e, "가속도계 센서 리스너 해제 실패")
                    }
                }
            }
    }
