package team.swyp.sdu.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * 기록 화면 ViewModel
 *
 * 사용자 정보, 친구 목록, 선택된 친구 상태를 관리합니다.
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RecordUiState>(RecordUiState.Loading)
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        // TODO: 친구 목록 로드 (FriendRepository 구현 후)
        loadFriends()
    }

    /**
     * 사용자 정보 로드
     */
    private fun loadUser() {
        viewModelScope.launch {
            userRepository.getUser()
                .onSuccess { user ->
                    val currentState = _uiState.value
                    _uiState.value = when (currentState) {
                        is RecordUiState.Success -> currentState.copy(user = user)
                        else -> RecordUiState.Success(
                            user = user,
                            friends = emptyList(),
                            selectedFriendNickname = null,
                        )
                    }
                }
                .onError { exception, message ->
                    Timber.e(exception, "사용자 정보 로드 실패: $message")
                    val currentState = _uiState.value
                    _uiState.value = when (currentState) {
                        is RecordUiState.Success -> currentState.copy(user = null)
                        else -> RecordUiState.Error(
                            message = message ?: "사용자 정보를 불러올 수 없습니다",
                        )
                    }
                }
        }
    }

    /**
     * 친구 목록 로드
     * TODO: FriendRepository 구현 후 실제 API 호출로 변경
     */
    private fun loadFriends() {
        viewModelScope.launch {
            // 임시 Mock 데이터
            val mockFriends = listOf(
                Friend("1", "친구1", null),
                Friend("2", "친구2", null),
                Friend("3", "친구3", null),
                Friend("4", "친구4", null),
                Friend("5", "친구5", null),
            )

            val currentState = _uiState.value
            _uiState.value = when (currentState) {
                is RecordUiState.Success -> currentState.copy(friends = mockFriends)
                else -> RecordUiState.Success(
                    user = null,
                    friends = mockFriends,
                    selectedFriendNickname = null,
                )
            }
        }
    }

    /**
     * 친구 선택
     */
    fun selectFriend(nickname: String) {
        val currentState = _uiState.value
        if (currentState is RecordUiState.Success) {
            _uiState.value = currentState.copy(selectedFriendNickname = nickname)
        }
    }

    /**
     * 친구 선택 해제 (내 기록으로 돌아가기)
     */
    fun clearFriendSelection() {
        val currentState = _uiState.value
        if (currentState is RecordUiState.Success) {
            _uiState.value = currentState.copy(selectedFriendNickname = null)
        }
    }
}

/**
 * 기록 화면 UI 상태
 */
sealed class RecordUiState {
    data object Loading : RecordUiState()

    data class Success(
        val user: User? = null,
        val friends: List<Friend> = emptyList(),
        val selectedFriendNickname: String? = null,
    ) : RecordUiState()

    data class Error(
        val message: String,
    ) : RecordUiState()
}

