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
        } catch (t: Throwable) {
            Timber.e(t, "주간 미션 조회 실패")
            Result.Error(t)
        }
    }

    override suspend fun getAllWeeklyMissions(): Result<List<WeeklyMission>> {
        return try {
            val response = missionRemoteDataSource.getAllWeeklyMissions()
            val weeklyMissions = WeeklyMissionMapper.toDomainList(response)
            Timber.d("주간 미션 목록 조회 성공: ${weeklyMissions.size}개")
            Result.Success(weeklyMissions)
        } catch (t: Throwable) {
            Timber.e(t, "주간 미션 목록 조회 실패")
            Result.Error(t)
        }
    }

    override suspend fun getMonthlyCompletedMissions(year: Int, month: Int): Result<List<String>> {
        return try {
            missionRemoteDataSource.getMonthlyCompletedMissions(year, month)
        } catch (t: Throwable) {
            Timber.e(t, "월간 미션 완료 목록 조회 실패")
            Result.Error(t, t.message)
        }
    }

    override suspend fun verifyWeeklyMissionReward(userWeeklyMissionId: Long): Result<WeeklyMission> {
        return try {
            val verifiedMissionDto = missionRemoteDataSource.verifyWeeklyMissionReward(userWeeklyMissionId)
            val verifiedMission = WeeklyMissionMapper.toDomain(verifiedMissionDto)
            Timber.d("주간 미션 보상 검증 성공: $userWeeklyMissionId")
            Result.Success(verifiedMission)
        } catch (t: Throwable) {
            Timber.e(t, "주간 미션 보상 검증 실패: $userWeeklyMissionId")
            Result.Error(t)
        }
    }
}
