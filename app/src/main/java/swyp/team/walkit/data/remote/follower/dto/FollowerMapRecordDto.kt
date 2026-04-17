package swyp.team.walkit.data.remote.follower.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 지도용 팔로워 산책 기록 DTO
 * GET /maps/follower/walking-records API 응답 항목
 */
@Serializable
data class FollowerMapRecordDto(
    @SerialName("userId")
    val userId: Long,

    @SerialName("walkId")
    val walkId: Long,

    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("responseCharacterDto")
    val responseCharacterDto: ResponseCharacterDto,
)

/**
 * 지도용 캐릭터 정보 DTO
 */
@Serializable
data class ResponseCharacterDto(
    @SerialName("grade")
    val grade: String = "SEED",

    @SerialName("headImageName")
    val headImageName: String? = null,

    @SerialName("bodyImageName")
    val bodyImageName: String? = null,
)
