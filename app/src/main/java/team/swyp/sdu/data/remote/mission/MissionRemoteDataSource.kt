package team.swyp.sdu.data.remote.mission

import javax.inject.Inject
import javax.inject.Singleton
import team.swyp.sdu.data.api.mission.MissionApi
import team.swyp.sdu.data.dto.mission.WeeklyMissionData
import timber.log.Timber

/**
 * 미션 정보를 서버에서 가져오는 데이터 소스
 */
@Singleton
class MissionRemoteDataSource @Inject constructor(
    private val missionApi: MissionApi,
) {
    /**
     * 주간 미션 목록 조회
     *
     * @return 주간 미션 목록
     */
    suspend fun getWeeklyMissions(): List<WeeklyMissionData> {
        return try {
            val missions = missionApi.getWeeklyMissions()
            Timber.d("주간 미션 조회 성공: ${missions.size}개")
            missions
        } catch (e: Exception) {
            Timber.e(e, "주간 미션 조회 실패")
            throw e
        }
    }
}





