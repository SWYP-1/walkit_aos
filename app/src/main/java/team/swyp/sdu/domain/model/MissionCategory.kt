package team.swyp.sdu.domain.model

/**
 * 미션 카테고리 enum
 * category와 type을 조합하여 더 세분화된 카테고리를 지원
 */
enum class MissionCategory(
    val displayName: String,
    val apiValue: String,
    val supportedTypes: List<String> = emptyList(), // 지원하는 MissionType apiValue들
) {
//    PHOTO("사진", "PHOTO", listOf("PHOTO_COLOR")),
    CHALLENGE_STEPS("걸음 수", "CHALLENGE", listOf("CHALLENGE_STEPS")),
    CHALLENGE_ATTENDANCE("연속 출석", "CHALLENGE", listOf("CHALLENGE_ATTENDANCE"));

    companion object {
        fun fromApiValue(apiValue: String): MissionCategory? {
            return entries.find { it.apiValue == apiValue }
        }

        fun fromTypeAndCategory(category: String, type: String): MissionCategory? {
            return entries.find { it.apiValue == category && it.supportedTypes.contains(type) }
                ?: entries.find { it.apiValue == category } // fallback to basic category
        }

        fun getAllCategories(): List<MissionCategory> {
            return entries.toList()
        }

        // UI 필터링용 카테고리들 (중복 제거)
        fun getFilterCategories(): List<MissionCategory> {
            return entries.distinctBy { it.displayName }
        }
    }
}








