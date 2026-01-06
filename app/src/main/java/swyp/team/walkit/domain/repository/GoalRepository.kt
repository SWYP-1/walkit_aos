package swyp.team.walkit.domain.repository

import kotlinx.coroutines.flow.StateFlow
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.Goal

/**
 * 목표 정보 Repository 인터페이스
 */
interface GoalRepository {
    val goalFlow: StateFlow<Goal?>

    /**
     * 목표 조회 (GET /goals)
     * 로컬 캐시를 우선 확인하고, 없으면 서버에서 가져옵니다.
     */
    suspend fun getGoal(): Result<Goal>
    
    /**
     * 목표 생성 (POST /goals)
     * 목표를 처음 생성할 때 사용 (온보딩 등)
     */
    suspend fun createGoal(goal: Goal): Result<Goal>
    
    /**
     * 목표 수정 (PUT /goals)
     * 기존 목표를 수정할 때 사용
     */
    suspend fun updateGoal(goal: Goal): Result<Goal>
    
    /**
     * 목표 갱신 (GET /goals)
     * 서버에서 강제로 최신 데이터를 가져와서 로컬 캐시를 업데이트합니다.
     * Pull-to-refresh나 동기화가 필요한 경우 사용합니다.
     */
    suspend fun refreshGoal(): Result<Goal>
}

