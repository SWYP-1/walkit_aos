package swyp.team.walkit.domain.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.FollowerLatestWalkRecord
import swyp.team.walkit.domain.model.FollowerMapRecord
import swyp.team.walkit.domain.model.FollowerRecentActivity

/**
 * 지도용 팔로워 산책 기록 Repository 인터페이스
 */
interface FollowerMapRepository {
    /**
     * 반경 내 팔로워 산책 기록 목록을 조회한다.
     *
     * @param lat    현재 위치 위도
     * @param lon    현재 위치 경도
     * @param radius 검색 반경 (미터 단위, 기본값: 1000)
     */
    suspend fun getFollowerWalkingRecords(
        lat: Double,
        lon: Double,
        radius: Int = 1000,
    ): Result<List<FollowerMapRecord>>

    /**
     * 팔로우 목록을 최근 산책 순으로 조회한다.
     */
    suspend fun getFollowerRecentActivities(): Result<List<FollowerRecentActivity>>

    /**
     * 특정 팔로워의 가장 최근 산책 기록 상세를 조회한다.
     *
     * @param userId 조회할 팔로워의 userId
     */
    suspend fun getFollowerLatestWalkRecord(userId: Long): Result<FollowerLatestWalkRecord>
}
