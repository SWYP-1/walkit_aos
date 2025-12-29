package team.swyp.sdu.domain.model

/**
 * 미션 카테고리 enum
 */
enum class MissionCategory(
    val displayName: String,
    val apiValue: String,
) {
    CHALLENGE("챌린지", "CHALLENGE"),
    PHOTO_COLOR("색깔찾기", "PHOTO_COLOR");

    companion object {
        fun fromApiValue(apiValue: String): MissionCategory? {
            return entries.find { it.apiValue == apiValue }
        }

        fun getAllCategories(): List<MissionCategory> {
            return entries.toList()
        }
    }
}








