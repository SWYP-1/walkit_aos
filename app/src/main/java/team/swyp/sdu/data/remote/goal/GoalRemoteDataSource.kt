package team.swyp.sdu.data.remote.goal

import javax.inject.Inject
import javax.inject.Singleton
import team.swyp.sdu.data.api.goal.GoalApi
import team.swyp.sdu.data.remote.goal.dto.RemoteGoalDto
import team.swyp.sdu.domain.model.Goal
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
        } catch (e: Exception) {
            Timber.e(e, "목표 조회 실패")
            throw e
        }
    }

    /**
     * 목표 생성 (POST /goals)
     * 목표를 처음 생성할 때 사용 (온보딩 등)
     */
    suspend fun createGoal(goal: Goal): Goal {
        return try {
            val requestDto = RemoteGoalDto(
                targetStepCount = goal.targetStepCount,
                targetWalkCount = goal.targetWalkCount,
            )
            val response = goalApi.createGoal(requestDto)
            
            if (response.isSuccessful) {
                // 응답 본문이 있으면 사용, 없으면 요청한 데이터 사용
                goal
            } else {
                // 응답이 실패했지만 요청한 데이터를 반환 (로컬 저장용)
                Timber.w("목표 생성 응답 실패 (코드: ${response.code()}), 요청한 데이터 사용")
                goal
            }
        } catch (e: Exception) {
            Timber.e(e, "목표 생성 실패")
            throw e
        }
    }

    /**
     * 목표 수정 (PUT /goals)
     * 기존 목표를 수정할 때 사용
     */
    suspend fun updateGoal(goal: Goal): Goal {
        return try {
            val requestDto = RemoteGoalDto(
                targetStepCount = goal.targetStepCount,
                targetWalkCount = goal.targetWalkCount,
            )
            val response = goalApi.updateGoal(requestDto)
            
            if (response.isSuccessful) {
                // 응답 본문이 있으면 사용, 없으면 요청한 데이터 사용
                goal
            } else {
                // 응답이 실패했지만 요청한 데이터를 반환 (로컬 저장용)
                Timber.w("목표 수정 응답 실패 (코드: ${response.code()}), 요청한 데이터 사용")
                goal
            }
        } catch (e: Exception) {
            Timber.e(e, "목표 수정 실패")
            throw e
        }
    }
}

