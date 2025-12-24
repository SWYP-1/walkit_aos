package team.swyp.sdu.data.remote.notification.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알림 아이템 DTO
 */
@Serializable
data class NotificationItemDto(
    @SerialName("notificationId")
    val notificationId: Int,
    
    @SerialName("type")
    val type: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("body")
    val body: String,
    
    @SerialName("senderId")
    val senderId: Int? = null,
    
    @SerialName("senderNickname")
    val senderNickname: String? = null,
    
    @SerialName("targetId")
    val targetId: String? = null,
    
    @SerialName("createdAt")
    val createdAt: String? = null,
    
    @SerialName("read")
    val read: Boolean = false,
)

