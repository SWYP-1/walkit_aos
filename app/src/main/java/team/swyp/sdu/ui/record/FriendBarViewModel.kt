package team.swyp.sdu.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.repository.FriendRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * 친구 목록 상단 바 ViewModel
 * 이벤트 기반 친구 목록 관리 (RecordScreen 상단 가로 리스트용)
 */
@HiltViewModel
class FriendBarViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
) : ViewModel() {

    /**
     * 친구 목록 상태 (StateFlow로 노출)
     * WhileSubscribed(5_000) 사용으로 화면이 활성화된 동안만 구독 유지
     */
    val friendsState: StateFlow<Result<List<Friend>>> = friendRepository.friendsState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Result.Loading
        )

    init {
        // 초기 데이터 로드 (캐시 우선)
        loadFriends(force = false)

        // 친구 상태 변경 이벤트 수신 및 자동 갱신
        viewModelScope.launch {
            friendRepository.friendUpdated.collect {
                Timber.d("친구 상태 변경 이벤트 수신 - 강제 갱신")
                loadFriends(force = true)
            }
        }
    }

    /**
     * 친구 목록 로드
     * @param force true: 캐시 무시하고 서버 재호출, false: 캐시 우선
     */
    private fun loadFriends(force: Boolean) {
        viewModelScope.launch {
            friendRepository.loadFriends(force)
        }
    }

    /**
     * 친구 목록 수동 갱신 (화면 진입/탭 이동 시 호출)
     * 캐시를 우선적으로 사용하되, TTL이 만료된 경우 서버 호출
     */
    fun refreshFriendsIfNeeded() {
        loadFriends(force = false)
    }
}

