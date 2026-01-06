package swyp.team.walkit.domain.service

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

/**
 * Step Counter 센서를 관리하는 클래스
 *
 * Android의 Step Counter 센서를 사용하여 걸음 수를 측정합니다.
 * Step Counter는 누적 걸음 수를 제공하며, 배터리 효율이 높습니다.
 */
@Singleton
class StepCounterManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sensorManager: SensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        private val stepCounterSensor: Sensor? =
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        private var initialStepCount: Float = 0f
        private var isTracking = false

        /**
         * Step Counter 센서 사용 가능 여부 확인
         */
        fun isStepCounterAvailable(): Boolean = stepCounterSensor != null

        /**
         * 걸음 수 추적 시작
         * 시작 시점의 걸음 수를 기준점으로 저장합니다.
         */
        fun startTracking() {
            if (!isStepCounterAvailable()) {
                Timber.w("Step Counter 센서를 사용할 수 없습니다")
                return
            }

            if (isTracking) {
                Timber.d("이미 걸음 수 추적 중입니다")
                return
            }

            // 초기 걸음 수를 저장 (기준점)
            // 첫 번째 센서 이벤트에서 초기값을 설정하기 위해 별도 처리 필요
            isTracking = true
            Timber.d("걸음 수 추적 시작")
        }

        /**
         * 걸음 수 추적 중지
         */
        fun stopTracking() {
            isTracking = false
            initialStepCount = 0f
            Timber.d("걸음 수 추적 중지")
        }

        /**
         * 초기 걸음 수 설정 (기준점)
         */
        fun setInitialStepCount(count: Float) {
            if (initialStepCount == 0f) {
                initialStepCount = count
                Timber.d("초기 걸음 수 설정: $initialStepCount")
            }
        }

        /**
         * 현재 걸음 수 (기준점 대비 증가량)
         */
        fun getCurrentStepCount(currentCount: Float): Int =
            if (initialStepCount > 0) {
                (currentCount - initialStepCount).toInt().coerceAtLeast(0)
            } else {
                0
            }

        /**
         * 걸음 수 업데이트를 Flow로 제공
         */
        fun getStepCountUpdates(): Flow<Int> =
            callbackFlow {
                if (!isStepCounterAvailable()) {
                    Timber.e("Step Counter 센서를 사용할 수 없습니다")
                    close()
                    return@callbackFlow
                }

                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent?) {
                            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER && isTracking) {
                                val totalSteps = event.values[0]

                                // 첫 번째 이벤트에서 초기값 설정
                                if (initialStepCount == 0f) {
                                    setInitialStepCount(totalSteps)
                                }

                                val stepCount = getCurrentStepCount(totalSteps)
                                trySend(stepCount)
                                Timber.d("걸음 수 업데이트: $stepCount")
                            }
                        }

                        override fun onAccuracyChanged(
                            sensor: Sensor?,
                            accuracy: Int,
                        ) {
                            // 정확도 변경 시 처리 불필요
                        }
                    }

                sensorManager.registerListener(
                    listener,
                    stepCounterSensor,
                    SensorManager.SENSOR_DELAY_UI,
                )

                awaitClose {
                    sensorManager.unregisterListener(listener)
                }
            }
    }
