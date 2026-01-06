package swyp.team.walkit.data.api.auth

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import swyp.team.walkit.data.remote.walking.dto.CharacterDto

/**
 * 캐릭터 정보 API
 */
interface CharacterApi {


    /**
     * 위치 기반 캐릭터 정보 조회
     *
     * @param lat 위도
     * @param lon 경도
     * @return 캐릭터 정보
     */
    @GET("/characters/walks")
    suspend fun getCharacterByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<CharacterDto>
}
