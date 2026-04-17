package swyp.team.walkit.domain.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.NearbySpot

/**
 * 장소(Spot) Repository 인터페이스
 */
interface SpotRepository {

    /**
     * 사용자 주변의 추천 장소 목록을 반환한다.
     *
     * @param query  검색 키워드 (예: "카페", "공원")
     * @param x      중심 경도 (longitude)
     * @param y      중심 위도 (latitude)
     * @param radius 검색 반경 (미터 단위, 최대 20000)
     * @param size   결과 개수 (최대 15)
     * @param sort   정렬 기준 (`distance` / `accuracy`)
     */
    suspend fun getNearbySpots(
        query: String,
        x: Double,
        y: Double,
        radius: Int,
        size: Int,
        sort: String,
    ): Result<List<NearbySpot>>
}