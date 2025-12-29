package team.swyp.sdu.domain.service

/**
 * 걸음수 수용 여부를 결정하는 게이트 인터페이스
 * 실시간으로 걸음수를 필터링하여 어뷰징을 방지합니다.
 */
interface StepGate {
    fun shouldAcceptStep(
        movementState: MovementState?,
        activityType: ActivityType?,
        gpsSpeedMps: Float,
        gpsMovedDistance: Float,
        stepDelta: Int,
    ): Boolean
}

/**
 * 기본 StepGate 구현체
 * 차량/자전거 이동, 제자리 흔들기, 비정상적인 step burst를 필터링합니다.
 */
class DefaultStepGate : StepGate {

    override fun shouldAcceptStep(
        movementState: MovementState?,
        activityType: ActivityType?,
        gpsSpeedMps: Float,
        gpsMovedDistance: Float,
        stepDelta: Int,
    ): Boolean {
        // 1. 차량/자전거 Activity → 무조건 차단
        if (activityType == ActivityType.IN_VEHICLE || activityType == ActivityType.ON_BICYCLE) {
            return false
        }

        // 2. 제자리 흔들기: 움직임은 있는데 GPS 이동 없음
        if (
            (movementState == MovementState.WALKING || movementState == MovementState.RUNNING) &&
            gpsMovedDistance < 2f &&
            gpsSpeedMps < 0.3f
        ) {
            return false
        }

        // 3. 물리적으로 말 안 되는 step burst 방지 (1초에 20보 이상)
        if (stepDelta > 20) {
            return false
        }

        return true
    }
}








