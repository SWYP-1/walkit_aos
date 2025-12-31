package team.swyp.sdu.domain.model

/**
 * 캐릭터 도메인 모델
 */
data class Character(
    val headImageName: String? = null,
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
enum class CharacterPart(val assetId: String, val lottieAssetId: String, val ribbonAssetId: String = lottieAssetId) {
    HEAD("head", "head", "headribbon"),
    BODY("body", "body"),
    FEET("feet", "foot")
}


