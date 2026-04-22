package swyp.team.walkit.data.remote.follower.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import swyp.team.walkit.data.remote.home.dto.PointDto

/**
 * 팔로워 최근 산책 기록 상세 DTO
 * GET /maps/follower/{userId}/walking-records/latest API 응답
 */
@Serializable
data class FollowerLatestWalkRecordDto(
    @SerialName("level")
    val level: Int = 0,

    @SerialName("grade")
    val grade: String = "SEED",

    @SerialName("nickName")
    val nickName: String,

    @SerialName("responseWalkRecordDto")
    val responseWalkRecordDto: WalkRecordDetailDto,

    @SerialName("likeCount")
    val likeCount: Int = 0,

    @SerialName("liked")
    val liked: Boolean = false,
)

/**
 * 산책 기록 상세 내용 DTO
 */
@Serializable
data class WalkRecordDetailDto(
    @SerialName("createdDate")
    val createdDate: String? = null,

    @SerialName("imageUrl")
    val imageUrl: String? = null,

    @SerialName("points")
    val points: List<PointDto> = emptyList(),

    @SerialName("totalTime")
    val totalTime: Long = 0,

    @SerialName("stepCount")
    val stepCount: Int = 0,

    @SerialName("walkId")
    val walkId: Long = 0,
)
