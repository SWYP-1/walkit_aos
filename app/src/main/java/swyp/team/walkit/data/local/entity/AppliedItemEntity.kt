package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 현재 적용된 아이템 Entity
 *
 * @param category 아이템 카테고리 (ItemCategory.name)
 * @param productId Google Play 제품 ID
 */
@Entity(tableName = "applied_items")
data class AppliedItemEntity(
    @PrimaryKey
    val category: String,  // ItemCategory.name
    val productId: String,
)












