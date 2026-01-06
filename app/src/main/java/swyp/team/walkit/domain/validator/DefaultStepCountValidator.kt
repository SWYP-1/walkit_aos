package swyp.team.walkit.domain.validator

import swyp.team.walkit.domain.model.StepValidationInput
import swyp.team.walkit.domain.model.StepValidationResult
import swyp.team.walkit.domain.service.ActivityType
import swyp.team.walkit.domain.service.MovementState
import javax.inject.Inject
import kotlin.math.pow

/**
 * 기본 걸음 수 검증 구현체
 *
 * 판별 규칙:
 * - ActivityType ≠ WALKING/RUNNING → reject
 * - MovementState ≠ WALKING/RUNNING → reject
 * - GPS 이동 < 1.5m + acceleration > 2.5 → PHONE_SHAKE
 * - GPS speed > 3.5m/s + stepDelta == 0 → VEHICLE
 * - 보폭 < 30cm + GPS 위치 분산 작음 → STATIONARY_WALKING
 * - 통과 시 stepDelta 반환
 */
class DefaultStepCountValidator @Inject constructor() : StepCountValidator {
    companion object {
        // 제자리 걸음 감지용 임계값
        private const val STATIONARY_STRIDE_THRESHOLD = 0.3f // 30cm 미만
        private const val MIN_LOCATION_VARIANCE = 0.0001f // GPS 위치 분산 임계값
        private const val MIN_LOCATIONS_FOR_VARIANCE = 3 // 위치 분산 계산을 위한 최소 위치 포인트 수
    }

    override fun validate(input: StepValidationInput): StepValidationResult {
        // ActivityType 검증
        if (input.activityType != ActivityType.WALKING && input.activityType != ActivityType.RUNNING) {
            return StepValidationResult.Rejected.InvalidActivityType
        }

        // MovementState 검증
        if (input.movementState != MovementState.WALKING && input.movementState != MovementState.RUNNING) {
            return StepValidationResult.Rejected.InvalidMovementState
        }

        // 폰 흔들기 감지: GPS 이동 < 1.5m + acceleration > 2.5
        if (input.gpsDistance < 1.5f && input.acceleration > 2.5f) {
            return StepValidationResult.Rejected.PhoneShake
        }

        // 차량 이동 감지: GPS speed > 3.5m/s + stepDelta == 0
        if (input.gpsSpeed > 3.5f && input.stepDelta == 0) {
            return StepValidationResult.Rejected.VehicleMovement
        }

        // 제자리 걸음 감지: 보폭 작음 + GPS 위치 분산 작음
//        if (input.stepDelta > 0 && input.locations.size >= MIN_LOCATIONS_FOR_VARIANCE) {
//            // 보폭 계산: GPS 이동 거리 / 걸음 수 증가량
//            val stride = input.gpsDistance / input.stepDelta
//
//            // GPS 위치 분산 계산
//            val locationVariance = calculateLocationVariance(input.locations)
//
//            // 제자리 걸음 조건: 보폭 < 30cm + GPS 위치 분산 < 0.0001
//            if (stride > 0f && stride < STATIONARY_STRIDE_THRESHOLD && locationVariance < MIN_LOCATION_VARIANCE) {
//                return StepValidationResult.Rejected.StationaryWalking
//            }
//        }

        // 모든 검증 통과
        return StepValidationResult.Accepted(input.stepDelta)
    }

    /**
     * GPS 위치 분산 계산
     * 위치 포인트들이 평균 위치 주변에 얼마나 모여있는지 측정
     * 분산이 작으면 실제 이동이 거의 없음을 의미
     */
    private fun calculateLocationVariance(locations: List<swyp.team.walkit.data.model.LocationPoint>): Float {
        if (locations.size < 2) return 0f

        val latitudes = locations.map { it.latitude }
        val longitudes = locations.map { it.longitude }

        val latMean = latitudes.average()
        val lonMean = longitudes.average()

        val latVariance = latitudes.map { (it - latMean).pow(2) }.average()
        val lonVariance = longitudes.map { (it - lonMean).pow(2) }.average()

        return (latVariance + lonVariance).toFloat()
    }
}

