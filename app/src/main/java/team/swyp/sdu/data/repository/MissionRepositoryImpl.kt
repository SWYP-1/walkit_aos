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

    override suspend fun getActiveWeeklyMission(): Result<List<WeeklyMission>> {
        return try {
            val weeklyMissionsDto = missionRemoteDataSource.getActiveWeeklyMission()
            val weeklyMissions = weeklyMissionsDto.map { WeeklyMissionMapper.toDomain(it) }
            Result.Success(weeklyMissions)
        } catch (e: Exception) {
            Timber.e(e, "주간 미션 조회 실패")
            Result.Error(e)
        }
    }

    override suspend fun getAllWeeklyMissions(): Result<List<WeeklyMission>> {
        return try {
            val response = missionRemoteDataSource.getAllWeeklyMissions()
            val weeklyMissions = WeeklyMissionMapper.toDomainList(response)
            Timber.d("주간 미션 목록 조회 성공: ${weeklyMissions.size}개")
            Result.Success(weeklyMissions)
        } catch (e: Exception) {
            Timber.e(e, "주간 미션 목록 조회 실패")
            Result.Error(e)
        }
    }

    override suspend fun getMonthlyCompletedMissions(year: Int, month: Int): Result<List<String>> {
        return try {
            missionRemoteDataSource.getMonthlyCompletedMissions(year, month)
        } catch (e: Exception) {
            Timber.e(e, "월간 미션 완료 목록 조회 실패")
            Result.Error(e, e.message)
        }
    }
}
