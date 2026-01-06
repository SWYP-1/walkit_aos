package swyp.team.walkit.data.remote.goal

import javax.inject.Inject
import javax.inject.Singleton
import swyp.team.walkit.data.api.goal.GoalApi
import swyp.team.walkit.data.remote.goal.dto.RemoteGoalDto
import swyp.team.walkit.domain.model.Goal
import timber.log.Timber

/**
 * 목표 정보를 서버에서 가져오는 데이터 소스
 */
@Singleton
class GoalRemoteDataSource @Inject constructor(
    private val goalApi: GoalApi,
) {
    /**
     * 목표 조회 (GET /goals)
     */
    suspend fun fetchGoal(): Goal {
        return try {
            val dto = goalApi.getGoal()
            Timber.d("목표 조회 성공: 걸음=${dto.targetStepCount}, 산책=${dto.targetWalkCount}")
            dto.toDomain()
        } catch (t: Throwable) {
            Timber.e(t, "목표 조회 실패")
            throw t
        }
    }

    /**
     * 목표 생성 (POST /goals)
     * 목표를 처음 생성할 때 사용 (온보딩 등)
     */
    suspend fun createGoal(goal: Goal): Goal {
        val requestDto = RemoteGoalDto(
            targetStepCount = goal.targetStepCount,
            targetWalkCount = goal.targetWalkCount,
        )
        val response = goalApi.createGoal(requestDto)

        if (response.isSuccessful) {
            // 응답 본문이 있으면 사용, 없으면 요청한 데이터 사용
            return goal
        } else {
            // 응답이 실패한 경우 예외를 던져서 상위에서 에러 처리하도록 함
            Timber.w("목표 생성 응답 실패 (코드: ${response.code()})")
            throw retrofit2.HttpException(response)
        }
    }

    /**
     * 목표 수정 (PUT /goals)
     * 기존 목표를 수정할 때 사용
     */
    suspend fun updateGoal(goal: Goal): Goal {
        val requestDto = RemoteGoalDto(
            targetStepCount = goal.targetStepCount,
            targetWalkCount = goal.targetWalkCount,
        )
        val response = goalApi.updateGoal(requestDto)

        if (response.isSuccessful) {
            // 응답 본문이 있으면 사용, 없으면 요청한 데이터 사용
            return goal
        } else {
            // 응답이 실패한 경우 예외를 던져서 상위에서 에러 처리하도록 함
            Timber.w("목표 수정 응답 실패 (코드: ${response.code()})")
            throw retrofit2.HttpException(response)
        }
    }
}

