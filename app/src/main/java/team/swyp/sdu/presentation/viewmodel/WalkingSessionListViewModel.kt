package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 산책 기록 리스트 ViewModel
 */
@HiltViewModel
class WalkingSessionListViewModel
    @Inject
    constructor(
        private val walkingSessionRepository: WalkingSessionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<WalkingSessionListUiState>(WalkingSessionListUiState.Loading)
        val uiState: StateFlow<WalkingSessionListUiState> = _uiState.asStateFlow()

        init {
            loadSessions()
        }

        /**
         * 세션 목록 로드
         */
        fun loadSessions() {
            viewModelScope.launch {
                walkingSessionRepository
                    .getAllSessions()
                    .catch { e ->
                        Timber.e(e, "세션 목록 로드 실패")
                        _uiState.value =
                            WalkingSessionListUiState.Error(
                                message = e.message ?: "알 수 없는 오류가 발생했습니다",
                            )
                    }.collect { sessions ->
                        _uiState.value = WalkingSessionListUiState.Success(sessions = sessions)
                    }
            }
        }

        /**
         * 세션 삭제
         */
        fun deleteSession(sessionId: Long) {
            viewModelScope.launch {
                try {
                    walkingSessionRepository.deleteSession(sessionId)
                    // 삭제 후 자동으로 리스트가 업데이트됨 (Flow)
                } catch (e: Exception) {
                    Timber.e(e, "세션 삭제 실패: ID=$sessionId")
                }
            }
        }
    }

/**
 * 산책 기록 리스트 UI 상태
 */
sealed class WalkingSessionListUiState {
    data object Loading : WalkingSessionListUiState()

    data class Success(
        val sessions: List<WalkingSession>,
    ) : WalkingSessionListUiState()

    data class Error(
        val message: String,
    ) : WalkingSessionListUiState()
}
