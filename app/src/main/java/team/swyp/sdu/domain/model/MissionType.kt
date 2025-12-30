package team.swyp.sdu.domain.model

/**
 * 미션 타입 enum
 */
enum class MissionType(
    val apiValue: String,
) {
    PHOTO_COLOR("PHOTO_COLOR"),
    CHALLENGE_STEPS("CHALLENGE_STEPS"),
    CHALLENGE_ATTENDANCE("CHALLENGE_ATTENDANCE");

    companion object {
        fun fromApiValue(apiValue: String): MissionType? {
            return entries.find { it.apiValue == apiValue }
        }
    }
}








