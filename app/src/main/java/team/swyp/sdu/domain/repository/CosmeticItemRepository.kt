package team.swyp.sdu.domain.repository

import android.app.Activity
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.CosmeticItem

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
     * @param position 아이템 위치 필터 (HEAD, BODY, FEET). null이면 전체 조회
     * @return Result<List<CosmeticItem>>
     */

}









