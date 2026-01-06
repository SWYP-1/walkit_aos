package swyp.team.walkit.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 아이템 이미지 정보 DTO
 */
@Serializable
data class ItemImageDto(
    @SerialName("imageName")
    val imageName: String? = null,

    @SerialName("itemPosition")
    val itemPosition: String? = null,

    @SerialName("itemTag")
    val itemTag: String? = null,
)

/**
 * 캐릭터 정보 DTO
 */
@Serializable
data class CharacterDto(
    @SerialName("headImage")
    val headImage: ItemImageDto? = null,

    @SerialName("bodyImage")
    val bodyImage: ItemImageDto? = null,

    @SerialName("feetImage")
    val feetImage: ItemImageDto? = null,

    @SerialName("characterImageName")
    val characterImageName: String? = null,

    @SerialName("backgroundImageName")
    val backgroundImageName: String? = null,

    @SerialName("level")
    val level: Int = 1,

    @SerialName("grade")
    val grade: String = "SEED",

    @SerialName("nickName")
    val nickName: String? = null,

    @SerialName("currentGoalSequence")
    val currentGoalSequence: Int? = null,
)








