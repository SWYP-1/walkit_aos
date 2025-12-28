package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import team.swyp.sdu.data.local.entity.NotificationSettingsEntity
import team.swyp.sdu.data.local.entity.SyncState

/**
 * 알림 설정 동기화를 위한 DAO
 */
@Dao
interface NotificationSettingsDao {
    /**
     * 알림 설정 삽입 또는 업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: NotificationSettingsEntity)

    /**
     * 현재 알림 설정 조회
     */
    @Query("SELECT * FROM notification_settings_sync WHERE id = 1")
    suspend fun getSettings(): NotificationSettingsEntity?

    /**
     * 동기화 상태 업데이트
     */
    @Query("UPDATE notification_settings_sync SET syncState = :syncState, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateSyncState(
        syncState: SyncState,
        updatedAt: Long = System.currentTimeMillis(),
    )

    /**
     * 동기화되지 않은 설정 조회 (PENDING 또는 FAILED 상태)
     */
    @Query("SELECT * FROM notification_settings_sync WHERE id = 1 AND syncState IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedSettings(): NotificationSettingsEntity?

    /**
     * 모든 설정 삭제
     */
    @Query("DELETE FROM notification_settings_sync")
    suspend fun deleteAll()
}






