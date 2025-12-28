package team.swyp.sdu.domain.model

/**
 * 미션 타입 enum
 */
enum class MissionType(
    val apiValue: String,
) {
    CHALLENGE_STEPS("CHALLENGE_STEPS"),
    PHOTO_COLOR("PHOTO_COLOR");

    companion object {
        fun fromApiValue(apiValue: String): MissionType? {
            return entries.find { it.apiValue == apiValue }
        }
    }
}







