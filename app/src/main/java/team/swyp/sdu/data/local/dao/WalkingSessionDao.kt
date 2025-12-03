package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import team.swyp.sdu.data.local.entity.WalkingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WalkingSession
 *
 * WalkingSession 데이터베이스 작업을 위한 DAO입니다.
 */
@Dao
interface WalkingSessionDao {
    /**
     * 세션 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WalkingSessionEntity): Long

    /**
     * 모든 세션 조회 (Flow로 실시간 업데이트)
     */
    @Query("SELECT * FROM walking_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkingSessionEntity>>

    /**
     * ID로 세션 조회
     */
    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WalkingSessionEntity?

    /**
     * 세션 삭제
     */
    @Delete
    suspend fun delete(session: WalkingSessionEntity)

    /**
     * ID로 세션 삭제
     */
    @Query("DELETE FROM walking_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 세션 업데이트
     */
    @Update
    suspend fun update(session: WalkingSessionEntity)

    /**
     * 동기화되지 않은 세션 조회
     */
    @Query("SELECT * FROM walking_sessions WHERE isSynced = 0 ORDER BY startTime ASC")
    suspend fun getUnsyncedSessions(): List<WalkingSessionEntity>

    /**
     * 동기화 상태 업데이트
     */
    @Query("UPDATE walking_sessions SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(
        id: Long,
        isSynced: Boolean,
    )

    /**
     * 모든 세션 삭제
     */
    @Query("DELETE FROM walking_sessions")
    suspend fun deleteAll()
}
