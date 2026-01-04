package team.swyp.sdu.data.remote.mission

import javax.inject.Inject
import javax.inject.Singleton
import team.swyp.sdu.core.Result
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
        } catch (t: Throwable) {
            Timber.e(t, "주간 미션 조회 실패")
            throw t
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
        } catch (t: Throwable) {
            Timber.e(t, "주간 미션 목록 조회 실패")
            throw t
        }
    }

    /**
     * 월간 미션 완료 목록 조회
     *
     * @param year 조회할 연도
     * @param month 조회할 월 (1-12)
     * @return 월간 미션 완료 날짜 목록
     */
    suspend fun getMonthlyCompletedMissions(year: Int, month: Int): Result<List<String>> {
        return try {
            val response = missionApi.getMonthlyCompletedMissions(year, month)

            if (response.isSuccessful) {
                val dates = response.body()
                if (dates != null) {
                    Timber.d("월간 미션 완료 목록 조회 성공: ${year}년 ${month}월, ${dates.size}개")
                    Result.Success(dates)
                } else {
                    Timber.e("월간 미션 완료 목록 조회 실패: 응답 바디가 null")
                    Result.Error(Exception("응답 데이터가 없습니다"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "월간 미션 완료 목록 조회 실패"
                Timber.e("월간 미션 완료 목록 조회 실패: $errorMessage (코드: ${response.code()})")
                Result.Error(Exception("월간 미션 완료 목록 조회 실패: ${response.code()}"))
            }
        } catch (t: Throwable) {
            Timber.e(t, "월간 미션 완료 목록 조회 중 예외 발생")
            Result.Error(t)
        }
    }

    /**
     * 주간 미션 보상 검증
     *
     * @param userWeeklyMissionId 검증할 미션 ID
     * @return 검증된 미션 정보
     */
    suspend fun verifyWeeklyMissionReward(userWeeklyMissionId: Long): WeeklyMissionDto {
        return try {
            val verifiedMission = missionApi.verifyWeeklyMissionReward(userWeeklyMissionId)
            Timber.d("주간 미션 보상 검증 성공: $userWeeklyMissionId")
            verifiedMission
        } catch (t: Throwable) {
            Timber.e(t, "주간 미션 보상 검증 실패: $userWeeklyMissionId")
            throw t
        }
    }
}







