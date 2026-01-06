package swyp.team.walkit.domain.repository

import android.app.Activity
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.CosmeticItem

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
     * 코스메틱 아이템 목록 조회 (API)
     *
     * @param position 아이템 위치 필터 (HEAD, BODY, FEET). null이면 전체 조회
     * @return Result<List<CosmeticItem>>
     */
    suspend fun getCosmeticItems(position: String? = null): Result<List<CosmeticItem>>


    /**
     * 코스메틱 아이템 구매 (API)
     *
     * @param items 구매할 아이템 목록
     * @param totalPrice 총 가격
     * @return 구매 결과
     */
    suspend fun purchaseItems(items: List<CosmeticItem>, totalPrice: Int): Result<Unit>

    /**
     * 코스메틱 아이템 착용/해제 (API)
     *
     * @param itemId 아이템 ID
     * @param isWorn 착용 여부 (true: 착용, false: 해제)
     * @return 착용/해제 결과
     */
    suspend fun wearItem(itemId: Int, isWorn: Boolean): Result<Unit>
}









