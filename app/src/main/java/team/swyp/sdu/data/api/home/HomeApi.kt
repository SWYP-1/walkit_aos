package team.swyp.sdu.data.api.home

import retrofit2.http.GET
import retrofit2.http.Query
import team.swyp.sdu.data.remote.home.dto.HomeData

/**
 * 홈 화면 API
 */
interface HomeApi {
    /**
     * 홈 화면 데이터 조회
     *
     * @param lat 위도
     * @param lon 경도
     * @return 홈 화면 데이터
     */
    @GET("/pages/home")
    suspend fun getHomeData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): HomeData
}





