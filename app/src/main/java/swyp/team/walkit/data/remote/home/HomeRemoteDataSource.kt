package swyp.team.walkit.data.remote.home

import swyp.team.walkit.core.Result
import swyp.team.walkit.data.api.home.HomeApi
import swyp.team.walkit.data.remote.home.dto.HomeData
import swyp.team.walkit.data.remote.interceptor.AuthExpiredException
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
        } catch (t: AuthExpiredException) {
            // 토큰 만료로 인한 인증 실패 - 특별 처리
            val elapsedTime = System.currentTimeMillis() - startTime
            Timber.tag(TAG_PERFORMANCE).w("토큰 만료로 인한 Home API 인증 실패: ${elapsedTime}ms, lat=$lat, lon=$lon")
            Timber.w(t, "토큰이 만료되어 홈 데이터 조회 실패")
            Result.Error(t, "로그인이 필요합니다. 다시 로그인해주세요.")
        } catch (t: Throwable) {
            val elapsedTime = System.currentTimeMillis() - startTime
            Timber.tag(TAG_PERFORMANCE).e(t, "Home API 호출 실패: ${elapsedTime}ms, lat=$lat, lon=$lon")
            Timber.e(t, "홈 데이터 조회 실패")
            Result.Error(t, t.message ?: "홈 데이터를 불러오지 못했습니다")
        }
    }
}

