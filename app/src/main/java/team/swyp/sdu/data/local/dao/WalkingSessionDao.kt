package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import team.swyp.sdu.data.local.entity.WalkingSessionEntity
import team.swyp.sdu.data.local.entity.SyncState
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
    suspend fun insert(session: WalkingSessionEntity)

    /**
     * 모든 세션 조회 (Flow로 실시간 업데이트)
     */
    @Query("SELECT * FROM walking_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkingSessionEntity>>

    /**
     * 기간 내 세션 조회 (startTime 기준)
     */
    @Query(
        """
        SELECT * FROM walking_sessions
        WHERE startTime BETWEEN :startMillis AND :endMillis
        ORDER BY startTime DESC
        """,
    )
    fun getSessionsBetween(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<WalkingSessionEntity>>

    /**
     * ID로 세션 조회 (Flow로 실시간 관찰)
     */
    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    fun observeSessionById(id: String): Flow<WalkingSessionEntity?>

    /**
     * ID로 세션 조회
     */
    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): WalkingSessionEntity?

    /**
     * 세션 삭제
     */
    @Delete
    suspend fun delete(session: WalkingSessionEntity)

    /**
     * ID로 세션 삭제
     */
    @Query("DELETE FROM walking_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 세션 업데이트
     */
    @Update
    suspend fun update(session: WalkingSessionEntity)

    /**
     * 동기화되지 않은 세션 조회 (PENDING 또는 FAILED 상태)
     */
    @Query("SELECT * FROM walking_sessions WHERE syncState IN ('PENDING', 'FAILED') ORDER BY startTime ASC")
    suspend fun getUnsyncedSessions(): List<WalkingSessionEntity>

    /**
     * 동기화된 세션 조회 (SYNCED 상태)
     */
    @Query("SELECT * FROM walking_sessions WHERE syncState = 'SYNCED' ORDER BY startTime DESC")
    suspend fun getSyncedSessions(): List<WalkingSessionEntity>

    /**
     * 동기화 상태 업데이트 (SyncState 사용)
     */
    @Query("UPDATE walking_sessions SET syncState = :syncState WHERE id = :id")
    suspend fun updateSyncState(
        id: String,
        syncState: SyncState,
    )

    /**
     * 동기화 상태 업데이트 (기존 호환성을 위한 메서드, deprecated)
     * @deprecated SyncState를 직접 사용하는 updateSyncState를 사용하세요
     */
    @Deprecated("Use updateSyncState instead", ReplaceWith("updateSyncState(id, if (isSynced) SyncState.SYNCED else SyncState.PENDING)"))
    @Query("UPDATE walking_sessions SET syncState = CASE WHEN :isSynced = 1 THEN 'SYNCED' ELSE 'PENDING' END WHERE id = :id")
    suspend fun updateSyncStatus(
        id: String,
        isSynced: Boolean,
    )

    /**
     * 모든 세션 삭제
     */
    @Query("DELETE FROM walking_sessions")
    suspend fun deleteAll()

    /**
     * 총 걸음수 집계 (Flow로 실시간 업데이트)
     */
    @Query("SELECT COALESCE(SUM(stepCount), 0) FROM walking_sessions")
    fun getTotalStepCount(): Flow<Int>

    /**
     * 총 이동거리 집계 (Flow로 실시간 업데이트, 미터 단위)
     */
    @Query("SELECT COALESCE(SUM(totalDistance), 0.0) FROM walking_sessions")
    fun getTotalDistance(): Flow<Float>

    /**
     * 총 산책 시간 집계 (Flow로 실시간 업데이트, 밀리초 단위)
     */
    @Query("SELECT COALESCE(SUM(endTime - startTime), 0) FROM walking_sessions")
    fun getTotalDuration(): Flow<Long>
}
