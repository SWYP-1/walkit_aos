package team.swyp.sdu.data.api.cosmetic

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import team.swyp.sdu.data.remote.cosmetic.dto.CosmeticItemDto

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
}
