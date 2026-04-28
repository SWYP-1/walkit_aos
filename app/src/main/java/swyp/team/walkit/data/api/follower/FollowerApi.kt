package swyp.team.walkit.data.api.follower

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import swyp.team.walkit.data.remote.follower.dto.FollowerLatestWalkRecordDto
import swyp.team.walkit.data.remote.follower.dto.FollowerMapRecordDto
import swyp.team.walkit.data.remote.follower.dto.FollowerRecentActivityDto
import swyp.team.walkit.data.remote.user.dto.FriendListItemDto
import swyp.team.walkit.data.remote.walking.dto.FollowerWalkRecordDto

/**
 * 팔로워 관련 API
 */
interface FollowerApi {

    /**
     * 지도용 팔로워 산책 기록 위치 목록 조회
     *
     * @param lat    현재 위치 위도
     * @param lon    현재 위치 경도
     * @param radius 검색 반경 (미터 단위, 기본값: 1000)
     * @return 반경 내 팔로워 산책 기록 목록
     */
    @GET("/maps/follower/walking-records")
    suspend fun getFollowerWalkingRecordsForMap(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Int = 1000,
    ): List<FollowerMapRecordDto>

    /**
     * 팔로우 목록 최근 산책 활동 조회 (최근 산책 순)
     *
     * @return 팔로우 목록 (최근 산책한 순서)
     */
    @GET("/maps/follower/recent-activities")
    suspend fun getFollowerRecentActivities(): List<FollowerRecentActivityDto>

    /**
     * 팔로워 최근 산책 기록 상세 조회
     *
     * @param userId 조회할 팔로워의 userId
     * @return 해당 팔로워의 가장 최근 산책 기록 상세
     */
    @GET("/maps/follower/{userId}/walking-records/latest")
    suspend fun getFollowerLatestWalkRecord(
        @Path("userId") userId: Long,
    ): FollowerLatestWalkRecordDto
    /**
     * 팔로워 산책 기록 조회
     *
     * @param nickname 팔로워 닉네임 (null이면 내 최근 정보 조회)
     * @param lat 위도 (선택사항)
     * @param lon 경도 (선택사항)
     * @return 팔로워 산책 기록 정보
     */
    @GET("/walk/follower/{nickname}")
    suspend fun getFollowerWalkRecord(
        @Path("nickname") nickname: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
    ): Response<FollowerWalkRecordDto>

//    /**
//     * 내 최근 산책 기록 조회
//     *
//     * @param lat 위도 (선택사항)
//     * @param lon 경도 (선택사항)
//     * @return 내 최근 산책 기록 정보
//     */
//    @GET("/walk/follower/me")
//    suspend fun getMyRecentWalkRecord(
//        @Query("lat") lat: Double? = null,
//        @Query("lon") lon: Double? = null,
//    ): Response<FollowerWalkRecordDto>

    /**
     * 닉네임으로 사용자 팔로우
     *
     * @param nickname 팔로우할 사용자의 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @POST("/follows/following/nickname/{nickname}")
    suspend fun followUserByNickname(
        @Path("nickname") nickname: String
    ): Response<Unit>

    /**
     * 팔로우 요청 수락
     *
     * @param nickname 팔로우 요청을 수락할 사용자의 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @PATCH("/follows/nickname/{nickname}")
    suspend fun acceptFollowRequest(
        @Path("nickname") nickname: String
    ): Response<Unit>

    /**
     * 팔로우 요청 거절/삭제
     *
     * @param nickname 팔로우 요청을 거절할 사용자의 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @DELETE("/follows/follower/nickname/{nickname}")
    suspend fun rejectFollowRequest(
        @Path("nickname") nickname: String
    ): Response<Unit>

    /**
     * 팔로우 요청 거절/삭제
     *
     * @param nickname 차단할 사용자의 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @DELETE("/follows/nickname/{nickname}")
    suspend fun blockUser(
        @Path("nickname") nickname: String
    ): Response<Unit>



    @GET("/follows")
    suspend fun getFriends(
    ): List<FriendListItemDto>



}

