package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 팔로워 산책 기록 DTO
 * GET /walk/follower/{nickname} API 응답
 */
@Serializable
data class FollowerWalkRecordDto(
    @SerialName("characterDto")
    val characterDto: CharacterDto,

    @SerialName("walkProgressPercentage")
    val walkProgressPercentage: String? = null,

    @SerialName("walkId")
    val walkId : Long? = null,

    @SerialName("createdDate")
    val createdDate: String? = null,

    @SerialName("stepCount")
    val stepCount: Int = 0,

    @SerialName("totalDistance")
    val totalDistance: Double = 0.0,

    @SerialName("likeCount")
    val likeCount: Int = 0,

    @SerialName("liked")
    val liked: Boolean = false
)




