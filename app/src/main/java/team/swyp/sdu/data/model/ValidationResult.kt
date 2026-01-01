package team.swyp.sdu.data.model

/**
 * 세션 검증 결과
 */
data class ValidationResult(
    val flags: List<SuspicionFlag>,
    val action: ValidationAction,
    val shouldCountSteps: Boolean
)

/**
 * 의심 플래그 (심각도별)
 *
 * 원칙: 단일 지표로 판단하지 않고, 복합 조건으로 판단
 */
enum class SuspicionFlag(val severity: Severity, val effect: FlagEffect, val description: String) {
    // 세션 거부
    IMPOSSIBLE_STRIDE(
        Severity.CRITICAL,
        FlagEffect.REJECT_SESSION,
        "보폭이 물리적으로 불가능합니다"
    ),
    IMPOSSIBLE_SPEED(
        Severity.CRITICAL,
        FlagEffect.REJECT_SESSION,
        "차량 수준 속도가 감지되었습니다"
    ),
    EXCESSIVE_STEPS(
        Severity.CRITICAL,
        FlagEffect.REJECT_SESSION,
        "비현실적으로 많은 걸음수입니다"
    ),

    // 걸음수 제외
    VEHICLE_DETECTED(
        Severity.WARNING,
        FlagEffect.EXCLUDE_STEPS,
        "차량/자전거 이동이 감지되었습니다"
    ),
    STATIONARY_WALKING(
        Severity.WARNING,
        FlagEffect.EXCLUDE_STEPS,
        "제자리 걸음이 감지되었습니다"
    ),
    SHAKING_PATTERN(
        Severity.WARNING,
        FlagEffect.EXCLUDE_STEPS,
        "흔들기 패턴이 감지되었습니다"
    ),

    // 정보성
    INDOOR_SUSPECTED(
        Severity.INFO,
        FlagEffect.NONE,
        "실내 활동으로 추정됩니다"
    )
}

/**
 * 플래그 심각도
 */
enum class Severity {
    CRITICAL,  // 즉시 거부
    WARNING,   // 저장하지만 플래그
    INFO       // 정보성
}

/**
 * 플래그 효과
 */
enum class FlagEffect {
    NONE,           // 영향 없음
    EXCLUDE_STEPS,  // 걸음수 제외
    REJECT_SESSION  // 세션 거부
}

/**
 * 검증 액션
 */
enum class ValidationAction {
    ACCEPT,          // 정상 저장
    ACCEPT_FLAGGED,  // 저장하지만 의심 플래그 표시
    REJECT           // 거부
}









