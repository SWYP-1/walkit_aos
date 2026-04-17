package swyp.team.walkit.data.remote.follower.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import swyp.team.walkit.data.remote.walking.dto.ItemImageDto

/**
 * 팔로우 최근 활동 목록 DTO
 * GET /maps/follower/recent-activities API 응답 항목
 */
@Serializable
data class FollowerRecentActivityDto(
    @SerialName("userId")
    val userId: Long,

    @SerialName("nickName")
    val nickName: String,

    @SerialName("walkedYesterday")
    val walkedYesterday: Boolean = false,

    @SerialName("responseCharacterDto")
    val responseCharacterDto: ResponseCharacterDto,

    /** 머리 아이템 상세 (itemTag로 headtop/headdecor 슬롯 결정) */
    @SerialName("headImage")
    val headImage: ItemImageDto? = null,

    /** 몸통 아이템 상세 */
    @SerialName("bodyImage")
    val bodyImage: ItemImageDto? = null,
)
