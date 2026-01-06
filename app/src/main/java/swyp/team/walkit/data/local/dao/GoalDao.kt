package swyp.team.walkit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import swyp.team.walkit.data.local.entity.GoalEntity

/**
 * 목표 캐시 DAO
 * 
 * 단일 사용자 앱이므로 단일 Goal 인스턴스만 관리합니다.
 */
@Dao
interface GoalDao {
    @Query("SELECT * FROM goal LIMIT 1")
    fun observeGoal(): Flow<GoalEntity?>

    @Query("SELECT * FROM goal LIMIT 1")
    suspend fun getGoal(): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

    @Query("DELETE FROM goal")
    suspend fun clear()
}


