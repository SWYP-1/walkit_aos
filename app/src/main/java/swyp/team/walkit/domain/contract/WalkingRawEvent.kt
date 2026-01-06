package swyp.team.walkit.domain.contract

import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.domain.model.StepValidationResult
import swyp.team.walkit.domain.service.ActivityType
import swyp.team.walkit.domain.service.MovementState

/**
 * 산책 추적의 원시 이벤트들
 * Service에서 발생하는 모든 이벤트를 표현합니다.
 */
sealed interface WalkingRawEvent {

    /**
     * 걸음 수 업데이트 이벤트
     */
    data class StepCountUpdate(
        val rawStepCount: Int,
        val validationResult: StepValidationResult? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : WalkingRawEvent

    /**
     * 위치 업데이트 이벤트
     */
    data class LocationUpdate(
        val locations: List<LocationPoint>,
        val timestamp: Long = System.currentTimeMillis()
    ) : WalkingRawEvent

    /**
     * 가속도계 이벤트
     */
    data class AccelerometerUpdate(
        val acceleration: Float,
        val movementState: MovementState,
        val timestamp: Long = System.currentTimeMillis()
    ) : WalkingRawEvent

    /**
     * 활동 상태 변경 이벤트
     */
    data class ActivityStateChange(
        val activityType: ActivityType,
        val confidence: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : WalkingRawEvent

    /**
     * 산책 추적 시작 이벤트
     */
    data object TrackingStarted : WalkingRawEvent

    /**
     * 산책 추적 중지 이벤트
     */
    data object TrackingStopped : WalkingRawEvent

    /**
     * 산책 추적 일시정지 이벤트
     */
    data object TrackingPaused : WalkingRawEvent

    /**
     * 산책 추적 재개 이벤트
     */
    data object TrackingResumed : WalkingRawEvent

    /**
     * 오류 이벤트
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : WalkingRawEvent
}







