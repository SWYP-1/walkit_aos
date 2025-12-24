package team.swyp.sdu.domain.repository

import android.app.Activity
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.domain.model.CharacterCustomization
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.ItemCategory

/**
 * 구매 이벤트
 */
sealed class PurchaseEvent {
    /**
     * 구매 성공
     */
    data class Success(val productId: String) : PurchaseEvent()

    /**
     * 구매 대기 중 (결제 처리 중)
     */
    data class Pending(val productId: String) : PurchaseEvent()

    /**
     * 구매 실패
     */
    data class Error(val message: String) : PurchaseEvent()

    /**
     * 구매 취소
     */
    data object Canceled : PurchaseEvent()
}

/**
 * 캐릭터 꾸미기 아이템 Repository 인터페이스
 */
interface CosmeticItemRepository {
    /**
     * 구매 가능한 아이템 목록 조회
     */
    fun getAvailableItems(): Flow<List<CosmeticItem>>

    /**
     * 구매한 아이템 목록 조회
     */
    fun getPurchasedItems(): Flow<List<CosmeticItem>>

    /**
     * 구매 이벤트 Flow
     * (Repository에서 BillingManager의 구매 이벤트를 Flow로 노출)
     */
    fun purchaseEvents(): Flow<PurchaseEvent>

    /**
     * 아이템 구매 흐름 시작
     *
     * @param activity Activity (구매 흐름 시작에 필요)
     * @param productId 제품 ID
     * @return Result<Unit>
     */
    suspend fun startPurchaseFlow(activity: Activity, productId: String): Result<Unit>

    /**
     * 구매 완료 처리
     *
     * @param purchase 구매 정보
     * @return Result<Unit>
     */
    suspend fun handlePurchase(purchase: Purchase): Result<Unit>

    /**
     * 구매 소비 (소모성 아이템)
     *
     * @param purchaseToken 구매 토큰
     * @return Result<Unit>
     */
    suspend fun consumePurchase(purchaseToken: String): Result<Unit>

    /**
     * 아이템 적용
     *
     * @param productId 제품 ID
     * @param category 아이템 카테고리
     * @return Result<Unit>
     */
    suspend fun applyItem(productId: String, category: ItemCategory): Result<Unit>

    /**
     * 현재 적용된 아이템 조회
     */
    fun getAppliedItems(): Flow<CharacterCustomization>

    /**
     * 아이템 제거
     *
     * @param category 아이템 카테고리
     * @return Result<Unit>
     */
    suspend fun removeItem(category: ItemCategory): Result<Unit>
}








