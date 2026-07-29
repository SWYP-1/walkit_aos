package swyp.team.walkit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import swyp.team.walkit.data.local.entity.ActiveTrackingEntity

/**
 * 진행 중인 산책 세션 GPS 포인트 DAO
 */
@Dao
interface ActiveTrackingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveTrackingEntity)

    @Query("SELECT * FROM active_tracking WHERE id = 1")
    suspend fun get(): ActiveTrackingEntity?

    @Query(
        """UPDATE active_tracking SET
        locationsJson = :locationsJson,
        filteredLocationsJson = :filteredLocationsJson,
        lastFlushTime = :flushTime WHERE id = 1""",
    )
    suspend fun updateLocations(
        locationsJson: String,
        filteredLocationsJson: String,
        flushTime: Long,
    )

    @Query("DELETE FROM active_tracking")
    suspend fun clear()
}
