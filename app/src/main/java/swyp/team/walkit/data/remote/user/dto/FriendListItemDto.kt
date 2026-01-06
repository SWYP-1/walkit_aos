package swyp.team.walkit.data.remote.user.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 친구 목록 아이템 DTO
 */
@Serializable
data class FriendListItemDto(
    @SerialName("nickname")
    val nickname: String?,

    @SerialName("userId")
    val userId: Long,

    @SerialName("imageName")
    val userImageUrl: String? = null,
)








