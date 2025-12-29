package team.swyp.sdu.data.api.mission

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import team.swyp.sdu.data.remote.mission.dto.MonthlyCompletedMissionDto
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionDto
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionListResponse

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
    ): Response<MonthlyCompletedMissionDto>
}







