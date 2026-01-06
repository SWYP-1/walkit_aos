package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 알림 설정 동기화를 위한 Room Entity
 *
 * 서버 동기화 실패 시 재시도를 위해 사용됩니다.
 * 사용자는 하나의 알림 설정만 가지므로 id는 항상 1입니다.
 */
@Entity(tableName = "notification_settings_sync")
data class NotificationSettingsEntity(
    @PrimaryKey
    val id: Long = 1, // 항상 1 (사용자당 하나의 설정만 존재)
    val notificationEnabled: Boolean,
    val goalNotificationEnabled: Boolean,
    val missionNotificationEnabled: Boolean,
    val friendNotificationEnabled: Boolean,
    val marketingPushEnabled: Boolean,
    val syncState: SyncState,
    val updatedAt: Long = System.currentTimeMillis(), // 마지막 업데이트 시간
)








