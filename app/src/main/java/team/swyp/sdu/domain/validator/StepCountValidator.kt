package team.swyp.sdu.domain.validator

import team.swyp.sdu.domain.model.StepValidationInput
import team.swyp.sdu.domain.model.StepValidationResult

/**
 * 걸음 수 검증 인터페이스
 *
 * 걷기/러닝만 걸음수 인정하고,
 * 제자리 걷기, 폰 흔들기, 차량 이동을 차단합니다.
 */
interface StepCountValidator {
    /**
     * 걸음 수 검증 수행
     *
     * @param input 검증 입력 데이터
     * @return 검증 결과
     */
    fun validate(input: StepValidationInput): StepValidationResult
}








