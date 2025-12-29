package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.data.local.entity.AppliedItemEntity

/**
 * 적용된 아이템 DAO
 */
@Dao
interface AppliedItemDao {
    /**
     * 적용된 아이템 삽입/업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AppliedItemEntity)

    /**
     * 적용된 아이템 조회 (카테고리로)
     */
    @Query("SELECT * FROM applied_items WHERE category = :category")
    suspend fun getByCategory(category: String): AppliedItemEntity?

    /**
     * 모든 적용된 아이템 조회
     */
    @Query("SELECT * FROM applied_items")
    fun getAll(): Flow<List<AppliedItemEntity>>

    /**
     * 적용된 아이템 삭제 (카테고리로)
     */
    @Query("DELETE FROM applied_items WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    /**
     * 모든 적용된 아이템 삭제
     */
    @Query("DELETE FROM applied_items")
    suspend fun deleteAll()
}











