package swyp.team.walkit.data.remote.notification.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알림 설정 DTO
 */
@Serializable
data class NotificationSettingsDto(
    @SerialName("notificationEnabled")
    val notificationEnabled: Boolean,
    @SerialName("goalNotificationEnabled")
    val goalNotificationEnabled: Boolean,
    @SerialName("missionNotificationEnabled")
    val missionNotificationEnabled: Boolean,
    @SerialName("friendNotificationEnabled")
    val friendNotificationEnabled: Boolean,
    @SerialName("marketingPushEnabled")
    val marketingPushEnabled: Boolean,
)

