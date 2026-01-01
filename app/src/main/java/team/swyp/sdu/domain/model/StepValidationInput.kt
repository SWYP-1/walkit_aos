package team.swyp.sdu.domain.model

import team.swyp.sdu.domain.service.ActivityType
import team.swyp.sdu.domain.service.MovementState
import team.swyp.sdu.data.model.LocationPoint

/**
 * 걸음 수 검증을 위한 입력 데이터
 */
data class StepValidationInput(
    val stepDelta: Int, // 걸음 수 증가량
    val activityType: ActivityType?, // 활동 상태 타입
    val movementState: MovementState?, // 움직임 상태
    val gpsDistance: Float, // GPS 이동 거리 (미터)
    val gpsSpeed: Float, // GPS 속도 (m/s)
    val acceleration: Float, // 가속도 (m/s²)
    val locations: List<LocationPoint>, // 위치 포인트 리스트
)










