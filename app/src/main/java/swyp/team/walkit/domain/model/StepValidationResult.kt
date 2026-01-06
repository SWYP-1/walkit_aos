package swyp.team.walkit.domain.model

/**
 * 걸음 수 검증 결과
 */
sealed interface StepValidationResult {
    /**
     * 검증 통과 - 걸음 수 인정
     */
    data class Accepted(val stepDelta: Int) : StepValidationResult

    /**
     * 검증 실패 - 거부 사유
     */
    sealed interface Rejected : StepValidationResult {
        /**
         * 활동 타입이 걷기/달리기가 아님
         */
        data object InvalidActivityType : Rejected

        /**
         * 움직임 상태가 걷기/달리기가 아님
         */
        data object InvalidMovementState : Rejected

        /**
         * 폰 흔들기 감지
         */
        data object PhoneShake : Rejected

        /**
         * 차량 이동 감지
         */
        data object VehicleMovement : Rejected

        /**
         * 제자리 걸음 감지
         */
        data object StationaryWalking : Rejected
    }
}

