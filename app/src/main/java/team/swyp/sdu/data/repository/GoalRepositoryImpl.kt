package team.swyp.sdu.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.local.dao.GoalDao
import team.swyp.sdu.data.local.mapper.GoalMapper
import team.swyp.sdu.data.remote.goal.GoalRemoteDataSource
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.repository.GoalRepository
import timber.log.Timber

/**
 * 목표 정보 Repository 구현체
 *
 * - 메모리(StateFlow) + Room + Remote 병합
 */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val remoteDataSource: GoalRemoteDataSource,
    private val userRepository: team.swyp.sdu.domain.repository.UserRepository,
) : GoalRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val goalState = MutableStateFlow<Goal?>(null)
    override val goalFlow: StateFlow<Goal?> = goalState.asStateFlow()

    init {
        // 사용자가 로그아웃한 경우 Goal 초기화만 처리
        // 목표 로드는 필요한 화면(HomeScreen, GoalManagementScreen)에서만 호출
        userRepository.userFlow
            .onEach { user ->
                if (user == null) {
                    // 사용자가 로그아웃한 경우 Goal 초기화
                    goalState.value = null
                    try {
                        goalDao.clear()
                    } catch (e: Exception) {
                        Timber.e(e, "Goal 삭제 실패")
                    }
                }
            }
            .launchIn(scope)
    }

    override suspend fun getGoal(): Result<Goal> =
        withContext(Dispatchers.IO) {
            try {
                // 현재 사용자 정보 확인
                val currentUser = userRepository.userFlow.value
                if (currentUser == null) {
                    return@withContext Result.Error(Exception("사용자 정보가 없습니다"))
                }

                // 로컬에서 Goal 조회
                val entity = goalDao.getGoal()
                if (entity != null) {
                    val goal = GoalMapper.toDomain(entity)
                    goalState.value = goal
                    Result.Success(goal)
                } else {
                    // 로컬에 없으면 서버에서 가져오기
                    refreshGoal()
                }
            } catch (e: Exception) {
                Timber.e(e, "목표 조회 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun createGoal(goal: Goal): Result<Goal> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에 생성 (POST)
                val createdGoal = remoteDataSource.createGoal(goal)
                // 로컬에 저장
                goalDao.upsert(GoalMapper.toEntity(createdGoal))
                goalState.value = createdGoal
                
                Result.Success(createdGoal)
            } catch (e: Exception) {
                Timber.e(e, "목표 생성 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateGoal(goal: Goal): Result<Goal> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에 수정 (PUT)
                val updatedGoal = remoteDataSource.updateGoal(goal)
                
                // 로컬에 저장
                goalDao.upsert(GoalMapper.toEntity(updatedGoal))
                goalState.value = updatedGoal
                
                Result.Success(updatedGoal)
            } catch (e: Exception) {
                Timber.e(e, "목표 수정 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun refreshGoal(): Result<Goal> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에서 최신 데이터 가져오기
                val goal = remoteDataSource.fetchGoal()
                
                // 로컬에 저장
                goalDao.upsert(GoalMapper.toEntity(goal))
                goalState.value = goal
                
                Result.Success(goal)
            } catch (e: Exception) {
                Timber.e(e, "목표 갱신 실패")
                Result.Error(e, e.message)
            }
        }

}

