package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.data.local.entity.PurchasedItemEntity

/**
 * 구매한 아이템 DAO
 */
@Dao
interface PurchasedItemDao {
    /**
     * 구매한 아이템 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PurchasedItemEntity)

    /**
     * 구매한 아이템 업데이트
     */
    @Update
    suspend fun update(item: PurchasedItemEntity)

    /**
     * 구매한 아이템 조회 (productId로)
     */
    @Query("SELECT * FROM purchased_items WHERE productId = :productId")
    suspend fun getByProductId(productId: String): PurchasedItemEntity?

    /**
     * 모든 구매한 아이템 조회
     */
    @Query("SELECT * FROM purchased_items")
    fun getAll(): Flow<List<PurchasedItemEntity>>

    /**
     * 구매한 productId 목록 조회
     */
    @Query("SELECT productId FROM purchased_items WHERE isConsumed = 0")
    suspend fun getAllPurchasedProductIds(): List<String>

    /**
     * 구매한 아이템 삭제
     */
    @Query("DELETE FROM purchased_items WHERE productId = :productId")
    suspend fun delete(productId: String)
}








