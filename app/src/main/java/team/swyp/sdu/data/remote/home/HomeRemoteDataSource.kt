package team.swyp.sdu.data.remote.home

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.api.home.HomeApi
import team.swyp.sdu.data.remote.home.dto.HomeData
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRemoteDataSource @Inject constructor(
    private val homeApi: HomeApi,
) {
    companion object {
        private const val TAG_PERFORMANCE = "HomePerformance"
    }

    suspend fun getHomeData(lat: Double, lon: Double): Result<HomeData> {
        val startTime = System.currentTimeMillis()
        return try {
            val data = homeApi.getHomeData(lat, lon)
            val elapsedTime = System.currentTimeMillis() - startTime
            Timber.tag(TAG_PERFORMANCE).d("Home API 호출 완료: ${elapsedTime}ms, lat=$lat, lon=$lon")
            Result.Success(data)
        } catch (e: Exception) {
            val elapsedTime = System.currentTimeMillis() - startTime
            Timber.tag(TAG_PERFORMANCE).e(e, "Home API 호출 실패: ${elapsedTime}ms, lat=$lat, lon=$lon")
            Timber.e(e, "홈 데이터 조회 실패")
            Result.Error(e, e.message ?: "홈 데이터를 불러오지 못했습니다")
        }
    }
}

