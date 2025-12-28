package team.swyp.sdu.data.remote.mission

import javax.inject.Inject
import javax.inject.Singleton
import team.swyp.sdu.data.api.mission.MissionApi
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionDto
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionListResponse
import timber.log.Timber

/**
 * 미션 정보를 서버에서 가져오는 데이터 소스
 */
@Singleton
class MissionRemoteDataSource @Inject constructor(
    private val missionApi: MissionApi,
) {
    /**
     * 활성화 된 이번 주 주간 미션
     *
     * @return 활성화 주간 미션
     */
    suspend fun getActiveWeeklyMission(): List<WeeklyMissionDto> {
        return try {
            val missions = missionApi.getActiveWeeklyMission()
            Timber.d("주간 미션 조회 성공: ${missions.size}개")
            missions
        } catch (e: Exception) {
            Timber.e(e, "주간 미션 조회 실패")
            throw e
        }
    }

    /**
     * 활성화 된 이번 주 주간 미션
     *
     * @return 활성화 주간 미션
     */
    suspend fun getAllWeeklyMissions(): WeeklyMissionListResponse {
        return try {
            val missions = missionApi.getAllWeeklyMissions()
            Timber.d("주간 미션 목록 조회 성공: ${missions.others.size + 1}개")
            missions
        } catch (e: Exception) {
            Timber.e(e, "주간 미션 목록 조회 실패")
            throw e
        }
    }
}







