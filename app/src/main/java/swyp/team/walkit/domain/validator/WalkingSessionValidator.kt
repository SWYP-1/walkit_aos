package swyp.team.walkit.domain.validator

import swyp.team.walkit.data.model.*
import timber.log.Timber

/**
 * 산책 세션 검증 클래스
 *
 * 핵심 원칙:
 * 1. Reject는 극단적/물리적으로 불가능한 경우만 사용
 * 2. 대부분의 어뷰징은 걸음수 제외(EXCLUDE_STEPS)로 해결
 * 3. 실시간 필터링 + 세션 종료 검증 조합
 */
class WalkingSessionValidator {

    fun validate(session: WalkingSession): ValidationResult {
        val flags = mutableListOf<SuspicionFlag>()

        validatePhysical(session, flags)
        validateMovementPatterns(session, flags)

        val shouldCountSteps = flags.none {
            it.effect == FlagEffect.EXCLUDE_STEPS
        }

        val action = when {
            flags.any { it.effect == FlagEffect.REJECT_SESSION } -> ValidationAction.REJECT
            flags.isNotEmpty() -> ValidationAction.ACCEPT_FLAGGED
            else -> ValidationAction.ACCEPT
        }

        Timber.d("세션 검증 완료: action=$action, flags=${flags.map { it.name }}, shouldCountSteps=$shouldCountSteps")

        return ValidationResult(
            flags = flags,
            action = action,
            shouldCountSteps = shouldCountSteps
        )
    }

    private fun validatePhysical(
        session: WalkingSession,
        flags: MutableList<SuspicionFlag>
    ) {
        // 불가능한 보폭
        if (session.stepCount > 0 && session.totalDistance > 0f) {
            val stride = session.totalDistance / session.stepCount
            if (stride < 0.2f || stride > 2.0f) {
                flags += SuspicionFlag.IMPOSSIBLE_STRIDE
            }
        }

        // 불가능한 속도
        val speedKmh = session.averageSpeedKmh()
        if (speedKmh > 20f) {
            flags += SuspicionFlag.IMPOSSIBLE_SPEED
        }

        // 과도한 걸음수
        if (session.stepCount > 100_000) {
            flags += SuspicionFlag.EXCESSIVE_STEPS
        }
    }

    private fun validateMovementPatterns(
        session: WalkingSession,
        flags: MutableList<SuspicionFlag>
    ) {
        val gpsMoved = session.hasMeaningfulGpsMovement()
        val stride = session.averageStride()

        // 제자리 걸음 감지
        if (!gpsMoved && stride in 0.0f..0.3f && session.stepCount > 300) {
            flags += SuspicionFlag.STATIONARY_WALKING
        }

        // 흔들기 패턴 감지
        if (!gpsMoved && session.averageSpeedKmh() < 1f && stride < 0.25f) {
            flags += SuspicionFlag.SHAKING_PATTERN
        }

        // 차량/자전거 이동 감지 로직은 제거됨
    }
}
