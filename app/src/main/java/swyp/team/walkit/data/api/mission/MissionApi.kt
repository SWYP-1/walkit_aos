package swyp.team.walkit.data.api.mission

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import swyp.team.walkit.data.remote.mission.dto.mission.WeeklyMissionDto
import swyp.team.walkit.data.remote.mission.dto.mission.WeeklyMissionListResponse

/**
 * 미션 API
 */
interface MissionApi {
    /**
     * 주간 미션 목록 조회
     */
    @GET("/missions/weekly")
    suspend fun getActiveWeeklyMission(): List<WeeklyMissionDto>

    @GET("/missions/weekly/list")
    suspend fun getAllWeeklyMissions(): WeeklyMissionListResponse

    /**
     * 월간 미션 완료 목록 조회
     *
     * @param year 조회할 연도
     * @param month 조회할 월 (1-12)
     * @return 월간 미션 완료 날짜 목록
     */
    @GET("/missions/completed/monthly")
    suspend fun getMonthlyCompletedMissions(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<List<String>>

    /**
     * 주간 미션 보상 검증 요청
     *
     * @param userWeeklyMissionId 검증할 미션 ID
     * @return 검증된 미션 정보
     */
    @POST("/missions/weekly/verify/{userWeeklyMissionId}")
    suspend fun verifyWeeklyMissionReward(
        @Path("userWeeklyMissionId") userWeeklyMissionId: Long
    ): WeeklyMissionDto
}







