package team.swyp.sdu.data.remote.notification.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알림 목록 응답 DTO
 */
@Serializable
data class NotificationListDto(
    @SerialName("notifications")
    val notifications: List<NotificationItemDto> = emptyList(),
    
    @SerialName("total")
    val total: Int? = null,
)




