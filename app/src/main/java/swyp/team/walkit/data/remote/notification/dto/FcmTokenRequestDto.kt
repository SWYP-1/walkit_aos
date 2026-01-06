package swyp.team.walkit.data.remote.notification.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FCM 토큰 등록 요청 DTO
 */
@Serializable
data class FcmTokenRequestDto(
    @SerialName("token")
    val token: String,
    @SerialName("deviceType")
    val deviceType: String = "ANDROID",
    @SerialName("deviceId")
    val deviceId: String,
)

