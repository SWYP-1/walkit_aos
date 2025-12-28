package team.swyp.sdu.domain.estimator

import team.swyp.sdu.domain.service.MovementState
import javax.inject.Inject

/**
 * 걸음 수 보간 전담 클래스
 *
 * 실제 step sensor 업데이트 전까지 가속도 기반 추정
 * 실제 값이 오면 점진적 수렴
 */
class StepEstimator @Inject constructor() {
    private var lastRealStepCount: Int = 0
    private var lastRealStepCountTime: Long = 0L
    private var interpolatedStepCount: Int = 0
    private var estimatedStepsPerSecond: Float = 0f
    private var movementStartTime: Long = 0L

    /**
     * 실제 걸음 수 업데이트
     *
     * @param realStepCount 실제 걸음 수
     * @param timestamp 현재 시간 (밀리초)
     */
    fun onRealStepUpdated(realStepCount: Int, timestamp: Long) {
        if (realStepCount != lastRealStepCount) {
            // 실제 걸음 수가 업데이트됨 -> 보간된 값을 실제 값으로 점진적으로 수렴
            lastRealStepCount = realStepCount
            lastRealStepCountTime = timestamp

            // 보간된 값이 실제 값과 다르면 점진적으로 수렴
            if (interpolatedStepCount != realStepCount) {
                val diff = realStepCount - interpolatedStepCount
                // 차이의 30%씩 수렴 (부드러운 전환)
                interpolatedStepCount += (diff * 0.3f).toInt()
                if (kotlin.math.abs(diff) < 2) {
                    // 차이가 작으면 즉시 실제 값으로 설정
                    interpolatedStepCount = realStepCount
                }
            } else {
                interpolatedStepCount = realStepCount
            }
        }
    }

    /**
     * 걸음 수 추정
     *
     * @param movementState 움직임 상태
     * @param acceleration 가속도 (m/s²)
     * @param timestamp 현재 시간 (밀리초)
     * @return 추정된 걸음 수
     */
    fun estimate(
        movementState: MovementState,
        acceleration: Float,
        timestamp: Long,
    ): Int {
        // 움직임 상태에 따라 예상 초당 걸음 수 계산
        val newEstimatedStepsPerSecond =
            when (movementState) {
                MovementState.WALKING -> {
                    // 걷기: 가속도에 따라 1.5 ~ 2.5 걸음/초 추정
                    // 가속도 1.5~3.0 m/s² 범위를 1.5~2.5 걸음/초로 매핑
                    val normalizedAccel = ((acceleration - 1.5f) / 1.5f).coerceIn(0f, 1f)
                    1.5f + normalizedAccel * 1.0f // 1.5 ~ 2.5 걸음/초
                }

                MovementState.RUNNING -> {
                    // 달리기: 가속도에 따라 2.5 ~ 4.0 걸음/초 추정
                    // 가속도 3.0~5.0+ m/s² 범위를 2.5~4.0 걸음/초로 매핑
                    val normalizedAccel = ((acceleration - 3.0f) / 2.0f).coerceIn(0f, 1f)
                    2.5f + normalizedAccel * 1.5f // 2.5 ~ 4.0 걸음/초
                }

                MovementState.STILL -> {
                    // 정지: 0 걸음/초
                    0f
                }

                MovementState.UNKNOWN -> {
                    // 알 수 없음: 이전 값 유지 또는 0
                    estimatedStepsPerSecond * 0.9f // 점진적으로 감소
                }
            }

        // 움직임 시작 시간 추적
        if (movementState == MovementState.WALKING || movementState == MovementState.RUNNING) {
            if (movementStartTime == 0L) {
                movementStartTime = timestamp
            }
            estimatedStepsPerSecond = newEstimatedStepsPerSecond
        } else {
            // 정지 상태면 움직임 시작 시간 초기화
            movementStartTime = 0L
            estimatedStepsPerSecond = 0f
        }

        // 실제 걸음 수가 업데이트되지 않음 -> 가속도계 기반으로 예상 걸음 수 계산
        if (estimatedStepsPerSecond > 0f && movementStartTime > 0L) {
            val timeSinceLastUpdate = (timestamp - lastRealStepCountTime) / 1000f // 초 단위

            // 마지막 실제 걸음 수 업데이트 이후 경과 시간 동안 예상 걸음 수 추가
            val estimatedAdditionalSteps = (timeSinceLastUpdate * estimatedStepsPerSecond).toInt()

            // 보간된 걸음 수 = 마지막 실제 걸음 수 + 예상 추가 걸음 수
            interpolatedStepCount = lastRealStepCount + estimatedAdditionalSteps

            // 보간된 값이 실제 값보다 너무 많이 앞서지 않도록 제한 (최대 10걸음)
            val maxAhead = lastRealStepCount + 10
            if (interpolatedStepCount > maxAhead) {
                interpolatedStepCount = maxAhead
            }
        } else {
            // 움직임이 없으면 보간된 값을 실제 값으로 유지
            interpolatedStepCount = lastRealStepCount
        }

        return interpolatedStepCount
    }

    /**
     * 상태 초기화
     */
    fun reset() {
        lastRealStepCount = 0
        lastRealStepCountTime = 0L
        interpolatedStepCount = 0
        estimatedStepsPerSecond = 0f
        movementStartTime = 0L
    }

    /**
     * 현재 추정된 초당 걸음 수 반환
     */
    fun getEstimatedStepsPerSecond(): Float = estimatedStepsPerSecond
}








