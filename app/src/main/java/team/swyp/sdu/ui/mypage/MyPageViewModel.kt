package team.swyp.sdu.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.DataState
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.domain.repository.CharacterRepository
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.ui.mypage.model.StatsData
import team.swyp.sdu.ui.mypage.model.UserInfoData
import timber.log.Timber
import javax.inject.Inject


/**
 * 마이 페이지 UI 상태 (부분 상태 관리)
 */
data class MyPageUiState(
    val userInfo: DataState<UserInfoData> = DataState.Loading,
    val stats: DataState<StatsData> = DataState.Loading,
    val consecutiveDays: DataState<Int> = DataState.Loading,
)

/**
 * 탈퇴 상태
 */
sealed interface WithdrawState {
    data object Idle : WithdrawState
    data object Loading : WithdrawState
    data object Success : WithdrawState
    data class Error(val message: String) : WithdrawState
}

/**
 * 마이페이지 ViewModel
 * 각 데이터를 독립적으로 로드하여 일부 실패해도 다른 기능은 동작합니다.
 */
@HiltViewModel
class MyPageViewModel
@Inject
constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val userRepository: UserRepository,
    private val characterRepository: CharacterRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    // 탈퇴 상태
    private val _withdrawState = MutableStateFlow<WithdrawState>(WithdrawState.Idle)
    val withdrawState: StateFlow<WithdrawState> = _withdrawState.asStateFlow()

    init {
        // 사용자 정보 변경을 실시간으로 구독 (초기 데이터도 처리)
        observeUserChanges()
        // 통계 데이터 로드
        loadStats()
        // 연속 출석일 계산
        loadConsecutiveDays()
    }

    /**
     * 사용자 정보 변경을 실시간으로 관찰
     * Room 데이터가 변경되면 자동으로 UI 업데이트 (초기 데이터도 처리)
     */
    private fun observeUserChanges() {
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                if (user != null) {
                    // 캐릭터 등급 정보 로드 (API 호출 없이 DB만 조회)
                    var grade: Grade? = null
                    try {
                        val character = characterRepository.getCharacterFromDb(user.nickname ?: "")
                        if (character != null) {
                            grade = character.grade
                            Timber.d("캐릭터 등급 로드 성공: ${user.nickname} - $grade")
                        } else {
                            Timber.d("DB에 캐릭터 정보 없음: ${user.nickname}")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "캐릭터 등급 로드 중 예외 발생")
                    }

                    // UI 상태 업데이트
                    _uiState.update {
                        it.copy(
                            userInfo = DataState.Success(
                                UserInfoData(
                                    nickname = user.nickname,
                                    profileImageUrl = user.imageName,
                                    grade = grade
                                )
                            )
                        )
                    }
                } else {
                    // 사용자 데이터가 없을 때는 에러 상태로 설정
                    _uiState.update {
                        it.copy(
                            userInfo = DataState.Error("사용자 정보를 불러올 수 없습니다")
                        )
                    }
                }
            }
        }
    }


    /**
     * 누적 통계 로드
     * Flow를 combine하여 총 걸음수와 총 산책 시간을 함께 관리합니다.
     */
    private fun loadStats() {
        viewModelScope.launch {
            combine(
                walkingSessionRepository.getTotalStepCount(),
                walkingSessionRepository.getTotalDuration(),
            ) { totalSteps, totalDurationMs ->
                StatsData(
                    totalStepCount = totalSteps,
                    totalWalkingTime = totalDurationMs
                )
            }
                .catch { e ->
                    Timber.e(e, "누적 통계 로드 실패")
                    _uiState.update {
                        it.copy(
                            stats = DataState.Error(
                                e.message ?: "통계 정보를 불러올 수 없습니다"
                            )
                        )
                    }
                }
                .collect { statsData ->
                    _uiState.update {
                        it.copy(stats = DataState.Success(statsData))
                    }
                }
        }
    }

    /**
     * 연속 출석일 계산
     * Synced 상태의 산책 세션들을 기준으로 오늘부터 역순으로 연속 출석일을 계산합니다.
     */
    private fun loadConsecutiveDays() {
        viewModelScope.launch {
            try {
                // 동기화된 세션들만 가져오기
                val syncedSessions = walkingSessionRepository.getSyncedSessions()

                // 날짜별로 그룹화 (중복 제거)
                val uniqueDates = syncedSessions.map { session ->
                    // startTime을 YYYY-MM-DD 형식으로 변환
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date(session.startTime))
                    date
                }.toSet().sorted()

                // 오늘 날짜
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                // 연속 출석일 계산
                var consecutiveDays = 0
                var currentDate = today

                // 역순으로 연속된 날짜들을 찾음
                while (uniqueDates.contains(currentDate)) {
                    consecutiveDays++
                    // 하루 전 날짜로 이동
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(currentDate)!!
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
                    currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
                }

                Timber.d("연속 출석일 계산 완료: ${consecutiveDays}일 (동기화된 세션: ${syncedSessions.size}개)")

                _uiState.update {
                    it.copy(consecutiveDays = DataState.Success(consecutiveDays))
                }

            } catch (e: Exception) {
                Timber.e(e, "연속 출석일 계산 실패")
                _uiState.update {
                    it.copy(consecutiveDays = DataState.Error("연속 출석일 정보를 불러올 수 없습니다"))
                }
            }
        }
    }

    /**
     * 사용자 탈퇴
     * UI에서 호출하면 상태가 자동으로 업데이트됩니다.
     */
    fun withdraw() {
        viewModelScope.launch {
            _withdrawState.value = WithdrawState.Loading

            val result = try {
                userRepository.deleteUser()
            } catch (e: Exception) {
                Timber.e(e, "사용자 탈퇴 중 예외 발생")
                Result.Error(e, e.message ?: "사용자 탈퇴에 실패했습니다")
            }

            when (result) {
                is Result.Success -> {
                    if (result.data.isSuccessful) {
                        Timber.d("사용자 탈퇴 성공")
                        _withdrawState.value = WithdrawState.Success
                    } else {
                        Timber.e("사용자 탈퇴 실패: HTTP ${result.data.code()}")
                        _withdrawState.value = WithdrawState.Error("탈퇴 처리에 실패했습니다")
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "사용자 탈퇴 실패: ${result.message}")
                    _withdrawState.value = WithdrawState.Error(result.message ?: "탈퇴 처리에 실패했습니다")
                }
                Result.Loading -> {
                    // 이미 Loading 상태로 설정됨
                }
            }
        }
    }

    /**
     * 탈퇴 상태 초기화 (다이얼로그 닫기 등에서 사용)
     */
    fun resetWithdrawState() {
        _withdrawState.value = WithdrawState.Idle
    }
}