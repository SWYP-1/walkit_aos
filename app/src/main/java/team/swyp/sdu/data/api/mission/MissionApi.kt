package team.swyp.sdu.data.api.mission

import retrofit2.http.GET
import team.swyp.sdu.data.dto.mission.WeeklyMissionData

/**
 * 미션 API
 */
interface MissionApi {
    /**
     * 주간 미션 목록 조회
     */
    @GET("/missions/weekly")
    suspend fun getWeeklyMissions(): List<WeeklyMissionData>
}





