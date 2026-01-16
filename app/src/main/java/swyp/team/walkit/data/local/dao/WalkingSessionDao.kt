package swyp.team.walkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import swyp.team.walkit.data.local.entity.WalkingSessionEntity
import swyp.team.walkit.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * 최근 세션 감정 조회용 데이터 클래스
 * 메모리 효율을 위해 필요한 필드만 포함
 */
data class RecentSessionEmotion(
    val startTime: Long,
    val postWalkEmotion: String
)

/**
 * 감정별 카운트 통계용 데이터 클래스
 */
data class EmotionCount(
    val emotion: String,
    val count: Int
)

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
     * 이번 주 세션 조회 (DB 레벨 필터링으로 최적화)
     * @param weekStart 이번 주 시작 시간 (월요일 00:00:00)
     * @param weekEnd 이번 주 끝 시간 (일요일 23:59:59)
     */
    @Query("SELECT * FROM walking_sessions WHERE startTime BETWEEN :weekStart AND :weekEnd ORDER BY startTime DESC")
    fun getSessionsThisWeek(weekStart: Long, weekEnd: Long): Flow<List<WalkingSessionEntity>>

    /**
     * 최근 7개 세션 조회 (최적화된 쿼리)
     * recentEmotions 계산용으로 startTime과 postWalkEmotion만 필요
     * SYNCED 상태인 세션만 조회 (동기화 완료된 세션만 표시)
     */
    @Query("SELECT startTime, postWalkEmotion FROM walking_sessions WHERE userId = :userId AND syncState = 'SYNCED' ORDER BY startTime DESC LIMIT 7")
    fun getRecentSessionsForEmotions(userId: Long): Flow<List<RecentSessionEmotion>>

    /**
     * 기간 내 감정별 카운트 통계 조회 (DB 레벨 최적화)
     * dominantEmotion 계산용
     * SYNCED 상태인 세션만 조회 (동기화 완료된 세션만 통계에 포함)
     */
    @Query("""
        SELECT postWalkEmotion as emotion, COUNT(*) as count
        FROM walking_sessions
        WHERE userId = :userId AND startTime BETWEEN :startTime AND :endTime AND syncState = 'SYNCED'
        GROUP BY postWalkEmotion
        ORDER BY count DESC
        LIMIT 1
    """)
    fun getDominantEmotionInPeriod(userId: Long, startTime: Long, endTime: Long): EmotionCount?

    /**
     * 기간 내 세션 조회 (startTime 기준) - 현재 사용자만
     */
    @Query(
        """
        SELECT * FROM walking_sessions
        WHERE userId = :userId AND startTime BETWEEN :startMillis AND :endMillis
        ORDER BY startTime DESC
        """,
    )
    fun getSessionsBetweenForUser(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<WalkingSessionEntity>>

    /**
     * 기간 내 동기화된 세션만 조회 (SyncState.SYNCED)
     */
    @Query(
        """
        SELECT * FROM walking_sessions
        WHERE userId = :userId AND startTime BETWEEN :startMillis AND :endMillis
        AND syncState = 'SYNCED'
        ORDER BY startTime DESC
        """,
    )
    fun getSyncedSessionsBetweenForUser(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<WalkingSessionEntity>>

    /**
     * ID로 세션 조회 (Flow로 실시간 관찰) - 현재 사용자만
     */
    @Query("SELECT * FROM walking_sessions WHERE userId = :userId AND id = :id")
    fun observeSessionByIdForUser(userId: Long, id: String): Flow<WalkingSessionEntity?>


    /**
     * ID로 세션 조회 - 현재 사용자만
     */
    @Query("SELECT * FROM walking_sessions WHERE userId = :userId AND id = :id")
    suspend fun getSessionByIdForUser(userId: Long, id: String): WalkingSessionEntity?


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

    @Query("SELECT * FROM walking_sessions WHERE userId = :userId AND syncState IN ('PENDING', 'SYNCING','FAILED')")
    suspend fun getUnsyncedSessionsForUser(userId: Long): List<WalkingSessionEntity>

    /**
     * 동기화된 세션 조회 (SYNCED 상태)
     */
    @Query("SELECT * FROM walking_sessions WHERE syncState = 'SYNCED' ORDER BY startTime DESC")
    suspend fun getSyncedSessions(): List<WalkingSessionEntity>

    @Query("SELECT * FROM walking_sessions WHERE userId = :userId AND syncState = 'SYNCED'")
    suspend fun getSyncedSessionsForUser(userId: Long): List<WalkingSessionEntity>

    /**
     * 동기화 상태 업데이트 (SyncState 사용)
     */
    @Query("UPDATE walking_sessions SET syncState = :syncState WHERE id = :id")
    suspend fun updateSyncState(
        id: String,
        syncState: SyncState,
    )

    /**
     * 세션 동기화 완료 처리
     * syncState를 SYNCED로, isSynced를 true로 설정
     */
    @Query("UPDATE walking_sessions SET syncState = 'SYNCED', isSynced = 1 WHERE id = :id")
    suspend fun markSessionAsSynced(
        id: String,
    )

    /**
     * 동기화 상태 업데이트 (기존 호환성을 위한 메서드, deprecated)
     * @deprecated SyncState를 직접 사용하는 updateSyncState를 사용하세요
     */
    @Deprecated("Use updateSyncState instead", ReplaceWith("updateSyncState(id, if (isSynced) SyncState.SYNCED else SyncState.PENDING)"))
    @Query("UPDATE walking_sessions SET syncState = CASE WHEN :isSynced = 1 THEN 'SYNCED' ELSE 'PENDING' END, isSynced = :isSynced WHERE id = :id")
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
     * 비정상적인 데이터(10만 걸음 초과)는 제외
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN stepCount > 100000 THEN 0  -- 10만 걸음 초과는 제외
                WHEN stepCount < 0 THEN 0        -- 음수 걸음수는 제외
                ELSE stepCount
            END
        ), 0) FROM walking_sessions WHERE userId = :userId
    """)
    fun getTotalStepCount(userId: Long): Flow<Int>

    /**
     * 총 이동거리 집계 (Flow로 실시간 업데이트, 미터 단위)
     */
    @Query("SELECT COALESCE(SUM(totalDistance), 0.0) FROM walking_sessions")
    fun getTotalDistance(): Flow<Float>

    /**
     * 총 산책 시간 집계 (Flow로 실시간 업데이트, 밀리초 단위)
     * 비정상적인 데이터(24시간 이상)는 제외
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN (endTime - startTime) > 86400000 THEN 0  -- 24시간(86400000ms) 초과는 제외
                WHEN (endTime - startTime) < 0 THEN 0         -- 음수 시간은 제외
                ELSE (endTime - startTime)
            END
        ), 0) FROM walking_sessions WHERE userId = :userId
    """)
    fun getTotalDuration(userId: Long): Flow<Long>

    /**
     * 특정 사용자의 세션이 하나라도 존재하는지 확인 (효율적 조회용)
     */
    @Query("SELECT COUNT(*) > 0 FROM walking_sessions WHERE userId = :userId")
    suspend fun hasAnySessionsForUser(userId: Long): Boolean
}
