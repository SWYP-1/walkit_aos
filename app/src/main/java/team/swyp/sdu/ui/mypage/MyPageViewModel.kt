package team.swyp.sdu.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.swyp.sdu.core.DataState
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.data.repository.WalkingSessionRepository
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
)

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

    init {
        // 사용자 정보 변경을 실시간으로 구독 (초기 데이터도 처리)
        observeUserChanges()
        // 통계 데이터 로드
        loadStats()
    }

    /**
     * 사용자 정보 변경을 실시간으로 관찰
     * Room 데이터가 변경되면 자동으로 UI 업데이트 (초기 데이터도 처리)
     */
    private fun observeUserChanges() {
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                if (user != null) {
                    // 캐릭터 정보도 함께 로드 (비동기로 처리)
                    var grade: Grade? = null
                    try {
                        characterRepository.getCharacter(user.nickname ?: "")
                            .onSuccess { character ->
                                grade = character.grade
                            }
                            .onError { exception, message ->
                                Timber.w(exception, "캐릭터 정보 로드 실패: $message")
                                // 캐릭터 정보는 선택적이므로 실패해도 계속 진행
                            }
                    } catch (e: Exception) {
                        Timber.w(e, "캐릭터 정보 로드 중 예외 발생")
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
}