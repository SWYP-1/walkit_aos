package swyp.team.walkit.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import swyp.team.walkit.data.remote.dto.ApiErrorResponse
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.local.dao.GoalDao
import swyp.team.walkit.data.local.mapper.GoalMapper
import swyp.team.walkit.data.remote.goal.GoalRemoteDataSource
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.repository.GoalRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import java.io.IOException

/**
 * 목표 정보 Repository 구현체
 *
 * - 메모리(StateFlow) + Room + Remote 병합
 */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val remoteDataSource: GoalRemoteDataSource,
    private val userRepository: swyp.team.walkit.domain.repository.UserRepository,
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
                        // DB 삭제 실패는 치명적이지 않으므로 로깅만
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "getGoal")
                Timber.e(e, "목표 조회 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "getGoal")
                Timber.e(e, "목표 조회 실패: HTTP ${e.code()}")
                Result.Error(e, "목표를 불러올 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "createGoal")
                Timber.e(e, "목표 생성 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "createGoal")
                Timber.e(e, "목표 생성 실패: HTTP ${e.code()}")
                Result.Error(e, "목표 생성에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun updateGoal(goal: Goal): Result<Goal> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에 수정 (PUT)
                val updatedGoal = remoteDataSource.updateGoal(goal)

                // 로컬에 저장 (취소되지 않도록 NonCancellable 사용)
                withContext(NonCancellable) {
                    goalDao.upsert(GoalMapper.toEntity(updatedGoal))
                    goalState.value = updatedGoal
                }

                Result.Success(updatedGoal)
            } catch (e: CancellationException) {
                // Coroutine 취소는 정상적인 상황이므로 에러로 처리하지 않음
                Timber.w("목표 수정이 취소되었습니다 (사용자 액션으로 인한 화면 종료)")
                // 취소된 경우에도 성공으로 처리 (이미 서버 요청은 성공했으므로)
                Result.Success(goal) // 현재 goal을 그대로 반환
            } catch (e: HttpException) {
                // HTTP 에러인 경우 API 에러 응답 파싱
                val errorBody = e.response()?.errorBody()?.string()
                val apiError = errorBody?.let { Json.decodeFromString<ApiErrorResponse>(it) }

                Timber.e(e, "목표 수정 HTTP 실패: ${e.code()}, ${apiError?.message}")

                // 특정 에러 코드에 따른 처리
                when (apiError?.code) {
                    7001 -> Result.Error(
                        Exception("GOAL_UPDATE_NOT_ALLOWED"),
                        apiError.message ?: "목표 수정은 한 달에 한 번만 가능합니다."
                    )
                    else -> Result.Error(
                        Exception("HTTP_ERROR"),
                        apiError?.message ?: "목표 수정에 실패했습니다."
                    )
                }
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "updateGoal")
                Timber.e(e, "목표 수정 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "refreshGoal")
                Timber.e(e, "목표 갱신 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "refreshGoal")
                Timber.e(e, "목표 갱신 실패: HTTP ${e.code()}")
                Result.Error(e, "목표를 갱신할 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

}

