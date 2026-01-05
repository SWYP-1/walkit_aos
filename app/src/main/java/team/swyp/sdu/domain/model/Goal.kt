package team.swyp.sdu.domain.model

/**
 * 목표 도메인 모델
 *
 * 사용자의 산책 목표 정보를 포함합니다.
 * User와 완전히 분리된 독립적인 도메인 모델입니다.
 */
data class Goal(
    val targetStepCount: Int,
    val targetWalkCount: Int,
) {
    companion object {
        val EMPTY = Goal(
            targetStepCount = 1000,
            targetWalkCount = 1,
        )
    }
}


