package swyp.team.walkit.domain.model

/**
 * 사용자 기본 정보 모델
 */
data class UserProfile(
    val uid: String,
    val nickname: String,
    val clearedCount: Int,
    val point: Int = 0,
    val goalKmPerWeek: Double = 0.0,
    val birthYear: Int? = null,
    val goalProgressSessions: Int = 0,
    val goalProgressSteps: Int = 0,
) {
    companion object {
        val EMPTY =
            UserProfile(
                uid = "",
                nickname = "",
                clearedCount = 0,
                point = 0,
                goalKmPerWeek = 0.0,
                birthYear = null,
                goalProgressSessions = 0,
                goalProgressSteps = 0,
            )
    }
}



