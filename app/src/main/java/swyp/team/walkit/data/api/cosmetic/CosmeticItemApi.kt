package swyp.team.walkit.data.api.cosmetic

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import swyp.team.walkit.data.remote.cosmetic.dto.CosmeticItemDto
import swyp.team.walkit.data.remote.cosmetic.dto.PurchaseRequestDto
import swyp.team.walkit.data.remote.cosmetic.dto.PurchaseResponseDto
import swyp.team.walkit.data.remote.cosmetic.dto.WearItemRequest

/**
 * 코스메틱 아이템 API
 */
interface CosmeticItemApi {

    /**
     * 코스메틱 아이템 목록 조회
     *
     * @param position 아이템 위치 필터 (HEAD, BODY, FEET). null이면 전체 조회
     * @return 코스메틱 아이템 목록
     */
    @GET("/items")
    suspend fun getCosmeticItems(
        @Query("position") position: String? = null
    ): Response<List<CosmeticItemDto>>

    /**
     * 코스메틱 아이템 구매
     *
     * @param request 구매 요청 데이터
     * @return 구매 결과 (빈 응답)
     */
    @POST("/items")
    suspend fun purchaseItems(
        @Body request: PurchaseRequestDto
    ): Response<Unit>

    /**
     * 캐릭터 아이템 착용/해제
     *
     * @param itemId 아이템 ID
     * @param request 착용 상태
     * @return 성공 시 빈 응답
     */
    @PATCH("/characters/items/{itemId}")
    suspend fun wearItem(
        @Path("itemId") itemId: Int,
        @Body request: WearItemRequest
    ): Response<Unit>
}

