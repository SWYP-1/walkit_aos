package team.swyp.sdu.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 위치 좌표 포인트 DTO
 */
@Serializable
data class PointDto(
    @SerialName("latitude")
    val latitude: Double = 0.0,

    @SerialName("longitude")
    val longitude: Double = 0.0,

    @SerialName("timestampMillis")
    val timestampMillis: Long = 0,
)

/**
 * 유저 포인트 정보 DTO
 */
@Serializable
data class UserPointDto(
    @SerialName("point")
    val point: Int = 0,
)





