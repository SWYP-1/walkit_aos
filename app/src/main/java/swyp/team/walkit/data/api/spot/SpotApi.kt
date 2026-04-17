package swyp.team.walkit.data.api.spot

import retrofit2.http.GET
import retrofit2.http.Query
import swyp.team.walkit.data.remote.spot.dto.NearbySpotDto

/**
 * 장소(Spot) 관련 Retrofit API 인터페이스
 */
interface SpotApi {

    /**
     * 사용자 주변의 추천 장소를 조회한다.
     *
     * @param query  검색 키워드 (예: "카페", "공원")
     * @param x      중심 경도 (longitude)
     * @param y      중심 위도 (latitude)
     * @param radius 검색 반경 (미터 단위, 최대 20000)
     * @param size   결과 개수 (최대 15)
     * @param sort   정렬 기준 (`distance` / `accuracy`)
     */
    @GET("spots/nearby")
    suspend fun getNearbySpots(
        @Query("query") query: String,
        @Query("x") x: Double,
        @Query("y") y: Double,
        @Query("radius") radius: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
    ): List<NearbySpotDto>
}