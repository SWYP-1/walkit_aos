package swyp.team.walkit.domain.model

/**
 * 캐릭터 도메인 모델
 */
data class Character(
    val headImageName: String? = null,
    val headImageTag: String? = null,  // HEAD 영역의 tag 정보 (TOP/DECOR)
    val bodyImageName: String? = null,
    val feetImageName: String? = null,
    val characterImageName: String? = null,
    val backgroundImageName: String? = null,
    val level: Int = 1,
    val grade: Grade = Grade.SEED,
    val nickName: String = "게스트",
)

/**
 * Lottie 애니메이션의 asset 정보를 표현하는 모델
 */
data class LottieAsset(
    val id: String,              // Lottie JSON 내 asset ID (예: "head_asset")
    val currentImageData: ByteArray? = null  // 교체될 이미지 데이터, null이면 투명
)

/**
 * Lottie 캐릭터 표시 상태 모델
 */
data class LottieCharacterState(
    val baseJson: String,           // 원본 Lottie JSON 문자열
    val modifiedJson: String? = null, // asset 교체된 수정된 JSON (캐싱용)
    val assets: Map<String, LottieAsset> = emptyMap(), // assetId -> asset 데이터 매핑
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 캐릭터 파트 타입 열거형
 */
enum class CharacterPart(val assetId: String, vararg val lottieAssetIds: String) {
    HEAD("head", "headtop", "headdecor"),
    BODY("body", "body"),
    FEET("feet", "foot");

    /**
     * tags를 기반으로 적절한 Lottie assetId를 반환
     * @param tags 코스메틱 아이템의 태그들 (콤마로 구분)
     * @return 선택된 Lottie assetId
     */
    fun getLottieAssetId(tags: String? = null): String {
        // tags가 없는 경우 기본 assetId 반환
        if (tags.isNullOrBlank()) {
            return lottieAssetIds.first()
        }

        return when (this) {
            HEAD -> {
                when {
                    tags.contains("TOP", ignoreCase = true) -> "headtop"
                    tags.contains("DECOR", ignoreCase = true) -> "headdecor"
                    else -> lottieAssetIds.first() // 기본값
                }
            }
            BODY -> lottieAssetIds.first()
            FEET -> lottieAssetIds.first()
        }
    }

    companion object {
        /**
         * position 문자열로부터 CharacterPart를 찾음
         */
        fun fromPosition(position: String): CharacterPart? {
            return values().find { it.name.equals(position, ignoreCase = true) }
        }
    }
}


