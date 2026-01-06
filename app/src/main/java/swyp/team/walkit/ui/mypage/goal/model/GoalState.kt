package swyp.team.walkit.ui.mypage.goal.model

/**
 * 목표 관리 화면의 상태
 */
data class GoalState(
    val targetSteps: Int = 1000,
    val walkFrequency: Int = 1,
)

fun GoalState.hasChangesComparedTo(other: GoalState): Boolean {
    return this.targetSteps != other.targetSteps ||
            this.walkFrequency != other.walkFrequency
}