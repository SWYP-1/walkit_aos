package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.data.local.entity.MissionProgressEntity

/**
 * 미션 진행도 DAO
 */
@Dao
interface MissionProgressDao {
    @Query("SELECT * FROM mission_progress WHERE date = :date LIMIT 1")
    fun observeProgress(date: String): Flow<MissionProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MissionProgressEntity)

    @Query("DELETE FROM mission_progress WHERE date = :date")
    suspend fun deleteByDate(date: String)
}









