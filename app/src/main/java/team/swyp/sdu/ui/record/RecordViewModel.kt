package team.swyp.sdu.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.repository.FriendRepository
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordUiState>(RecordUiState.Loading)
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RecordUiEvent>()
    val uiEvent: SharedFlow<RecordUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadInitialData()
    }

    /**
     * 초기 데이터 로드
     */
    private fun loadInitialData() = viewModelScope.launch {
        _uiState.value = RecordUiState.Loading

        val userResult = userRepository.getUser()

        userResult
            .onSuccess { user ->
                _uiState.value = RecordUiState.Success(
                    user = user,
                    selectedFriendNickname = null,
                )
            }
            .onError { _, _ ->
                _uiState.value = RecordUiState.Error(
                    message = "사용자 정보를 불러올 수 없습니다."
                )
            }
    }

    /**
     * 친구 선택
     */
    fun selectFriend(nickname: String) {
        android.util.Log.d("RecordViewModel", "RecordViewModel.selectFriend 호출됨: $nickname")
        timber.log.Timber.d("RecordViewModel.selectFriend 호출됨: $nickname")

        val state = _uiState.value
        if (state is RecordUiState.Success) {
            val newState = state.copy(selectedFriendNickname = nickname)
            android.util.Log.d("RecordViewModel", "새로운 selectedFriendNickname: ${newState.selectedFriendNickname}")
            timber.log.Timber.d("새로운 selectedFriendNickname: ${newState.selectedFriendNickname}")

            _uiState.update { newState }
        } else {
            android.util.Log.d("RecordViewModel", "RecordUiState가 Success가 아님: $state")
            timber.log.Timber.d("RecordUiState가 Success가 아님: $state")
        }
    }

    /**
     * 친구 선택 해제
     */
    fun clearFriendSelection() {
        android.util.Log.d("RecordViewModel", "clearFriendSelection 호출됨")
        timber.log.Timber.d("clearFriendSelection 호출됨")

        val state = _uiState.value
        if (state is RecordUiState.Success) {
            val oldNickname = state.selectedFriendNickname
            val newState = state.copy(selectedFriendNickname = null)
            android.util.Log.d("RecordViewModel", "친구 선택 해제: $oldNickname -> null")
            timber.log.Timber.d("친구 선택 해제: $oldNickname -> null")

            _uiState.update { newState }
        } else {
            android.util.Log.d("RecordViewModel", "clearFriendSelection 실패: RecordUiState가 Success가 아님")
            timber.log.Timber.d("clearFriendSelection 실패: RecordUiState가 Success가 아님")
        }
    }


    /**
     * 친구 차단 (이미 친구인 유저)
     * Note: 친구 목록 갱신은 FriendBarViewModel에서 이벤트 기반으로 자동 처리됨
     */
    fun blockSelectedFriend(nickname : String) {
        val state = _uiState.value
        if (state !is RecordUiState.Success) return

        viewModelScope.launch {
            friendRepository.blockUser(nickname)
                .onSuccess {
                    _uiEvent.emit(
                        RecordUiEvent.ShowToast("차단되었습니다.")
                    )

                    // 차단 성공 시 친구 목록 이벤트 발행 (FriendBarViewModel에서 자동 갱신)
                    friendRepository.invalidateCache()
                    friendRepository.emitFriendUpdated()

                    _uiState.update {
                        state.copy(selectedFriendNickname = null)
                    }
                }
                .onError { throwable, message ->
                    Timber.e(throwable, "친구 차단 실패")
                    _uiEvent.emit(
                        RecordUiEvent.ShowToast(message.toString())
                    )
                }
        }
    }

    /**
     * UI State
     */
    sealed class RecordUiState {
        data object Loading : RecordUiState()

        data class Success(
            val user: User?,
            val selectedFriendNickname: String?,
        ) : RecordUiState()

        data class Error(
            val message: String,
        ) : RecordUiState()
    }

    /**
     * One-shot UI Event
     */
    sealed interface RecordUiEvent {
        data class ShowToast(val message: String) : RecordUiEvent
    }
}
