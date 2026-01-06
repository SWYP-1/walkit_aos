package swyp.team.walkit.domain.model

/**
 * 미션 타입 enum
 */
enum class MissionType {
    PHOTO_COLOR,
    CHALLENGE_STEPS,
    CHALLENGE_ATTENDANCE;

    companion object {
        fun fromApiValue(apiValue: String): MissionType {
            return when (apiValue) {
                "PHOTO_COLOR" -> PHOTO_COLOR
                "CHALLENGE_ATTENDANCE" -> CHALLENGE_ATTENDANCE
                else -> CHALLENGE_STEPS
            }
        }
    }
}







