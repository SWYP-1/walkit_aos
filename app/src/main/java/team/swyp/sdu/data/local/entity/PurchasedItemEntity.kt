package team.swyp.sdu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 구매한 아이템 Entity
 *
 * @param productId Google Play 제품 ID
 * @param purchaseToken 구매 토큰 (Google Play에서 발급)
 * @param purchaseTime 구매 시간 (밀리초)
 * @param quantity 수량
 * @param isConsumed 소비 여부 (소모성 아이템의 경우)
 */
@Entity(tableName = "purchased_items")
data class PurchasedItemEntity(
    @PrimaryKey
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val quantity: Int = 1,
    val isConsumed: Boolean = false,
)








