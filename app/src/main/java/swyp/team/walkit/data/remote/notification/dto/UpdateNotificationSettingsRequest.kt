package swyp.team.walkit.data.remote.notification.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알림 설정 업데이트 요청 DTO (PATCH용 - 변경된 필드만 포함)
 */
@Serializable
data class UpdateNotificationSettingsRequest(
    @SerialName("notificationEnabled")
    val notificationEnabled: Boolean? = null,
    @SerialName("goalNotificationEnabled")
    val goalNotificationEnabled: Boolean? = null,
    @SerialName("newMissionNotificationEnabled")
    val newMissionNotificationEnabled: Boolean? = null,
    @SerialName("friendNotificationEnabled")
    val friendNotificationEnabled: Boolean? = null,
    @SerialName("marketingPushEnabled")
    val marketingPushEnabled: Boolean? = null,
)

