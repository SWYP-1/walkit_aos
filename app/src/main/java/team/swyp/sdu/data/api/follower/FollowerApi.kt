package team.swyp.sdu.data.api.follower

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import team.swyp.sdu.data.remote.walking.dto.FollowerWalkRecordDto

/**
 * 팔로워 관련 API
 */
interface FollowerApi {
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

    /**
     * 내 최근 산책 기록 조회
     *
     * @param lat 위도 (선택사항)
     * @param lon 경도 (선택사항)
     * @return 내 최근 산책 기록 정보
     */
    @GET("/walk/follower/me")
    suspend fun getMyRecentWalkRecord(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
    ): Response<FollowerWalkRecordDto>

    /**
     * 닉네임으로 사용자 팔로우
     *
     * @param nickname 팔로우할 사용자의 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @POST("/follows/nickname/{nickname}")
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
    @DELETE("/follows/nickname/{nickname}")
    suspend fun rejectFollowRequest(
        @Path("nickname") nickname: String
    ): Response<Unit>
}

