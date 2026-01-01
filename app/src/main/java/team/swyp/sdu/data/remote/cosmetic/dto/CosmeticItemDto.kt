package team.swyp.sdu.data.remote.cosmetic.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 코스메틱 아이템 API 응답 DTO
 */
@Serializable
data class CosmeticItemDto(
    @SerialName("imageName")
    val imageName: String,
    @SerialName("itemId")
    val itemId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("owned")
    val owned: Boolean,
    @SerialName("position")
    val position: String,
    @SerialName("point")
    val point: Int,
    @SerialName("worn")
    val worn: Boolean? = null, // 선택적 필드
    @SerialName("tags")
    val tags: String? = null // 선택적 필드 - 서버에서 추가 예정
) {

    /**
     * Lottie assetId로 변환
     * position과 tags를 기반으로 적절한 assetId를 반환
     */
    fun toLottieAssetId(): String {
        return when (position.uppercase()) {
            "HEAD" -> {
                // tags에 따라 headtop 또는 headdocor 결정
                when {
                    tags?.contains("TOP", ignoreCase = true) == true -> "headtop"
                    tags?.contains("DECOR", ignoreCase = true) == true -> "headdocor"
                    else -> "headtop" // 기본값
                }
            }
            "BODY" -> "body"
            "FEET" -> "feet"
            else -> {
                // 알 수 없는 position의 경우 기본값
                "body"
            }
        }
    }

    companion object {
        // assetId 상수들
        const val ASSET_HEAD_TOP = "headtop"
        const val ASSET_HEAD_DECOR = "headdocor"
        const val ASSET_BODY = "body"
        const val ASSET_FEET = "feet"
    }
}


