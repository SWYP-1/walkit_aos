package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.utils.WalkingTestData
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
        fun deleteSession(sessionId: String) {
            viewModelScope.launch {
                try {
                    walkingSessionRepository.deleteSession(sessionId)
                    // 삭제 후 자동으로 리스트가 업데이트됨 (Flow)
                } catch (e: Exception) {
                    Timber.e(e, "세션 삭제 실패: ID=$sessionId")
                }
            }
        }

        /**
         * 11월 더미 데이터 생성 및 저장
         * 이미 11월 데이터가 있으면 생성하지 않습니다.
         */
        fun generateNovemberTestData() {
            viewModelScope.launch {
                try {
                    // 기존 세션 확인 (현재 UI 상태에서 확인)
                    val currentSessions = (_uiState.value as? WalkingSessionListUiState.Success)?.sessions ?: emptyList()
                    val hasNovemberData = currentSessions.any { session ->
                        val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        sessionDate.monthValue == 11
                    }
                    
                    // 11월 데이터가 없으면 생성
                    if (!hasNovemberData) {
                        // 현재 사용자 ID 가져오기
                        val currentUserId = walkingSessionRepository.getCurrentUserId()
                        Timber.d("📋 11월 더미 데이터 생성 - 현재 사용자 ID: $currentUserId")

                        val novemberSessions = WalkingTestData.generateNovemberSessions(userId = currentUserId) // ✅ userId 전달
                        Timber.d("11월 더미 데이터 생성 시작: ${novemberSessions.size}개 세션")

                        novemberSessions.forEach { session ->
                            Timber.d("💾 11월 세션 저장: userId=${session.userId}")
                            walkingSessionRepository.saveSession(session)
                        }

                        Timber.d("11월 더미 데이터 저장 완료: ${novemberSessions.size}개 세션 (userId=$currentUserId)")
                    } else {
                        Timber.d("11월 데이터가 이미 존재하여 생성하지 않습니다")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "11월 더미 데이터 생성 실패")
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
