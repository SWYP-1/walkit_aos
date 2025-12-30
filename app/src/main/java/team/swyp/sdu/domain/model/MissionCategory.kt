package team.swyp.sdu.domain.model

/**
 * 미션 카테고리 enum
 */
enum class MissionCategory(
    val displayName: String,
    val apiValue: String,
) {
    PHOTO("사진", "PHOTO"),
    CHALLENGE("챌린지", "CHALLENGE");

    companion object {
        fun fromApiValue(apiValue: String): MissionCategory? {
            return entries.find { it.apiValue == apiValue }
        }

        fun getAllCategories(): List<MissionCategory> {
            return entries.toList()
        }
    }
}








