package team.swyp.sdu.data.api.mission

import retrofit2.http.GET
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
}







