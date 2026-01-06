package swyp.team.walkit.domain.goal

data class GoalRange(
    val min: Int,
    val max: Int
) {
    fun isValid(value: Int): Boolean {
        return value in min..max
    }
}
