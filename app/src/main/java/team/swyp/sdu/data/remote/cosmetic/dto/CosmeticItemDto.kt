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
)
