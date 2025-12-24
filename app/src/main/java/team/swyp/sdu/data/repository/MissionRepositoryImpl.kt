package team.swyp.sdu.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.mission.MissionRemoteDataSource
import team.swyp.sdu.data.remote.mission.mapper.WeeklyMissionMapper
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.domain.repository.MissionRepository
import timber.log.Timber

/**
 * 미션 Repository 구현체
 */
@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val missionRemoteDataSource: MissionRemoteDataSource,
) : MissionRepository {

    override suspend fun getWeeklyMissions(): Result<List<WeeklyMission>> {
        return try {
            val weeklyMissionsDto = missionRemoteDataSource.getWeeklyMissions()
            val weeklyMissions = weeklyMissionsDto.map { WeeklyMissionMapper.toDomain(it) }
            Result.Success(weeklyMissions)
        } catch (e: Exception) {
            Timber.e(e, "주간 미션 조회 실패")
            Result.Error(e)
        }
    }
}
