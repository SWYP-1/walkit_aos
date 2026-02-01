package swyp.team.walkit.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.mission.MissionRemoteDataSource
import swyp.team.walkit.data.remote.mission.mapper.WeeklyMissionMapper
import swyp.team.walkit.domain.model.WeeklyMission
import swyp.team.walkit.domain.repository.MissionRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import retrofit2.HttpException
import java.io.IOException

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
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "getActiveWeeklyMission")
            Timber.e(e, "주간 미션 조회 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "getActiveWeeklyMission")
            Timber.e(e, "주간 미션 조회 실패: HTTP ${e.code()}")
            Result.Error(e, "주간 미션을 불러올 수 없습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }

    override suspend fun getAllWeeklyMissions(): Result<List<WeeklyMission>> {
        return try {
            val response = missionRemoteDataSource.getAllWeeklyMissions()
            val weeklyMissions = WeeklyMissionMapper.toDomainList(response)
            Timber.d("주간 미션 목록 조회 성공: ${weeklyMissions.size}개")
            Result.Success(weeklyMissions)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "getAllWeeklyMissions")
            Timber.e(e, "주간 미션 목록 조회 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "getAllWeeklyMissions")
            Timber.e(e, "주간 미션 목록 조회 실패: HTTP ${e.code()}")
            Result.Error(e, "주간 미션 목록을 불러올 수 없습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }

    override suspend fun getMonthlyCompletedMissions(year: Int, month: Int): Result<List<String>> {
        return try {
            missionRemoteDataSource.getMonthlyCompletedMissions(year, month)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "getMonthlyCompletedMissions")
            Timber.e(e, "월간 미션 완료 목록 조회 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "getMonthlyCompletedMissions")
            Timber.e(e, "월간 미션 완료 목록 조회 실패: HTTP ${e.code()}")
            Result.Error(e, "월간 미션 완료 목록을 불러올 수 없습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }

    override suspend fun verifyWeeklyMissionReward(userWeeklyMissionId: Long): Result<WeeklyMission> {
        return try {
            val verifiedMissionDto = missionRemoteDataSource.verifyWeeklyMissionReward(userWeeklyMissionId)
            val verifiedMission = WeeklyMissionMapper.toDomain(verifiedMissionDto)
            Timber.d("주간 미션 보상 검증 성공: $userWeeklyMissionId")
            Result.Success(verifiedMission)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "verifyWeeklyMissionReward")
            Timber.e(e, "주간 미션 보상 검증 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "verifyWeeklyMissionReward")
            Timber.e(e, "주간 미션 보상 검증 실패: HTTP ${e.code()}")
            Result.Error(e, "미션 보상 검증에 실패했습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }
}
