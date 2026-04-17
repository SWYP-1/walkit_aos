package swyp.team.walkit.data.remote.spot

import swyp.team.walkit.core.Result
import swyp.team.walkit.data.api.spot.SpotApi
import swyp.team.walkit.data.remote.exception.AuthExpiredException
import swyp.team.walkit.data.remote.spot.dto.NearbySpotDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 주변 장소 원격 데이터 소스
 *
 * [SpotApi]를 호출하고 결과를 [Result]로 래핑하여 반환한다.
 */
@Singleton
class SpotRemoteDataSource @Inject constructor(
    private val spotApi: SpotApi,
) {
    companion object {
        private const val TAG = "SpotRemoteDataSource"
    }

    /**
     * 사용자 주변의 추천 장소 목록을 조회한다.
     *
     * @param query  검색 키워드
     * @param x      중심 경도 (longitude)
     * @param y      중심 위도 (latitude)
     * @param radius 검색 반경 (미터 단위)
     * @param size   결과 개수
     * @param sort   정렬 기준
     */
    suspend fun getNearbySpots(
        query: String,
        x: Double,
        y: Double,
        radius: Int = 1000,
        size: Int = 15,
        sort: String = "distance",
    ): Result<List<NearbySpotDto>> =
        try {
            val result = spotApi.getNearbySpots(
                query = query,
                x = x,
                y = y,
                radius = radius,
                size = size,
                sort = sort,
            )
            Timber.tag(TAG).d("주변 장소 조회 성공: ${result.size}개, query=$query, x=$x, y=$y")
            Result.Success(result)
        } catch (t: AuthExpiredException) {
            Timber.tag(TAG).w(t, "토큰 만료로 인해 주변 장소 조회 실패")
            Result.Error(t, "로그인이 필요합니다. 다시 로그인해주세요.")
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "주변 장소 조회 실패: query=$query, x=$x, y=$y")
            Result.Error(t, t.message ?: "주변 장소를 불러오지 못했습니다.")
        }
}