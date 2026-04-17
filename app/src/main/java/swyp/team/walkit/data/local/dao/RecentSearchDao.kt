package swyp.team.walkit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import swyp.team.walkit.data.local.entity.RecentSearchEntity

/**
 * 최근 검색어 DAO
 */
@Dao
interface RecentSearchDao {

    /** 최신순으로 최대 [limit]개 조회 */
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 5): Flow<List<RecentSearchEntity>>

    /** 검색어 저장 (중복 시 searchedAt 갱신) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentSearchEntity)

    /** 특정 검색어 삭제 */
    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun delete(query: String)

    /** 전체 삭제 */
    @Query("DELETE FROM recent_searches")
    suspend fun deleteAll()

    /** 오래된 항목을 trim하여 최대 [limit]개만 유지 */
    @Query("DELETE FROM recent_searches WHERE query NOT IN (SELECT query FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit)")
    suspend fun trimToLimit(limit: Int = 5)
}
