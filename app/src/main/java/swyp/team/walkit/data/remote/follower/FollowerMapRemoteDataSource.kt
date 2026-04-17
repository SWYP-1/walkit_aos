package swyp.team.walkit.data.remote.follower

import swyp.team.walkit.core.Result
import swyp.team.walkit.data.api.follower.FollowerApi
import swyp.team.walkit.data.remote.exception.AuthExpiredException
import swyp.team.walkit.data.remote.follower.dto.FollowerLatestWalkRecordDto
import swyp.team.walkit.data.remote.follower.dto.FollowerMapRecordDto
import swyp.team.walkit.data.remote.follower.dto.FollowerRecentActivityDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지도용 팔로워 산책 기록 원격 데이터 소스
 *
 * [FollowerApi]를 호출하고 결과를 [Result]로 래핑하여 반환한다.
 */
@Singleton
class FollowerMapRemoteDataSource @Inject constructor(
    private val followerApi: FollowerApi,
) {
    companion object {
        private const val TAG = "FollowerMapRemoteDataSource"
    }

    /**
     * 지정한 좌표 반경 내의 팔로워 산책 기록 목록을 조회한다.
     *
     * @param lat    현재 위치 위도
     * @param lon    현재 위치 경도
     * @param radius 검색 반경 (미터 단위)
     */
    suspend fun getFollowerWalkingRecords(
        lat: Double,
        lon: Double,
        radius: Int = 1000,
    ): Result<List<FollowerMapRecordDto>> =
        try {
            val result = followerApi.getFollowerWalkingRecordsForMap(
                lat = lat,
                lon = lon,
                radius = radius,
            )
            Timber.tag(TAG).d("팔로워 산책 기록 조회 성공: ${result.size}개, lat=$lat, lon=$lon")
            Result.Success(result)
        } catch (t: AuthExpiredException) {
            Timber.tag(TAG).w(t, "토큰 만료로 인해 팔로워 산책 기록 조회 실패")
            Result.Error(t, "로그인이 필요합니다. 다시 로그인해주세요.")
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "팔로워 산책 기록 조회 실패: lat=$lat, lon=$lon")
            Result.Error(t, t.message ?: "팔로워 산책 기록을 불러오지 못했습니다.")
        }

    /**
     * 팔로우 목록을 최근 산책 순으로 조회한다.
     */
    suspend fun getFollowerRecentActivities(): Result<List<FollowerRecentActivityDto>> =
        try {
            val result = followerApi.getFollowerRecentActivities()
            Timber.tag(TAG).d("팔로우 최근 활동 조회 성공: ${result.size}개")
            Result.Success(result)
        } catch (t: AuthExpiredException) {
            Timber.tag(TAG).w(t, "토큰 만료로 인해 팔로우 최근 활동 조회 실패")
            Result.Error(t, "로그인이 필요합니다. 다시 로그인해주세요.")
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "팔로우 최근 활동 조회 실패")
            Result.Error(t, t.message ?: "팔로우 목록을 불러오지 못했습니다.")
        }

    /**
     * 특정 팔로워의 가장 최근 산책 기록 상세를 조회한다.
     *
     * @param userId 조회할 팔로워의 userId
     */
    suspend fun getFollowerLatestWalkRecord(userId: Long): Result<FollowerLatestWalkRecordDto> =
        try {
            val result = followerApi.getFollowerLatestWalkRecord(userId)
            Timber.tag(TAG).d("팔로워 최근 산책 상세 조회 성공: userId=$userId")
            Result.Success(result)
        } catch (t: AuthExpiredException) {
            Timber.tag(TAG).w(t, "토큰 만료로 인해 팔로워 최근 산책 상세 조회 실패")
            Result.Error(t, "로그인이 필요합니다. 다시 로그인해주세요.")
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "팔로워 최근 산책 상세 조회 실패: userId=$userId")
            Result.Error(t, t.message ?: "산책 기록을 불러오지 못했습니다.")
        }
}
