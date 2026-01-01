package team.swyp.sdu.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 알림 아이템 도메인 모델
 */
@Parcelize
data class NotificationItem(
    val notificationId: Int,
    val type: NotificationType,
    val title: String,
    val body: String,
    val senderId: Long? = null,
    val senderNickname: String? = null,
    val targetId: String? = null,
    val createdAt: String? = null,
    val read: Boolean = false,
) : Parcelable
