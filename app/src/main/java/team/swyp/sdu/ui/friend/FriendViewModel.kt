package team.swyp.sdu.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.friend.FollowRemoteDataSource
import team.swyp.sdu.data.remote.user.AlreadyFollowingException
import team.swyp.sdu.data.remote.user.FollowRequestAlreadyExistsException
import team.swyp.sdu.data.remote.user.FollowSelfException
import team.swyp.sdu.data.remote.user.FollowUserNotFoundException
import team.swyp.sdu.data.remote.user.UserNotFoundException
import team.swyp.sdu.data.remote.user.UserRemoteDataSource
import team.swyp.sdu.data.remote.user.UserSearchResult
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.repository.FriendRepository
import timber.log.Timber
import android.content.SharedPreferences
import android.app.Application
import javax.inject.Inject

/**
 * 친구 목록 UI 상태
 */
sealed interface FriendUiState {
    data object Loading : FriendUiState
    data class Success(val friends: List<Friend>) : FriendUiState
    data class Error(val message: String?) : FriendUiState
}

/**
 * 검색 UI 상태
 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val result: UserSearchResult) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class FriendViewModel
@Inject
constructor(
    private val application: Application,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val followRemoteDataSource: FollowRemoteDataSource,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FriendUiState>(FriendUiState.Loading)
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    // 팔로우 상태 공유를 위한 SharedPreferences
    private val followPrefs: SharedPreferences by lazy {
        application.getSharedPreferences("follow_status_prefs", android.content.Context.MODE_PRIVATE)
    }

    // 자동 검색은 제거 (FriendSearchScreen에서 수동 검색만 사용)

    val filteredFriends: StateFlow<List<Friend>> =
        combine(_friends, _query) { list, q ->
            val keyword = q.trim()
            if (keyword.isBlank()) list
            else list.filter { it.nickname.contains(keyword, ignoreCase = true) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        loadFriends()
    }

    /**
     * 친구 목록 로드
     */
    fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = FriendUiState.Loading
            when (val result = friendRepository.loadFriends()) {
                is Result.Success -> {
                    _friends.value = result.data
                    _uiState.value = FriendUiState.Success(result.data)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "친구 목록 로드 실패")
                    _uiState.value = FriendUiState.Error(result.message)
                }
                Result.Loading -> {} // 이미 Loading 상태
            }
        }
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
        _searchUiState.value = SearchUiState.Idle
    }

    /**
     * 닉네임으로 사용자 검색
     */
    fun searchUser(nickname: String) {
        val trimmedNickname = nickname.trim()
        if (trimmedNickname.isBlank()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val result = userRemoteDataSource.searchUserByNickname(trimmedNickname)
                // FriendSearchViewModel에서 저장된 팔로우 상태 확인
                val localFollowStatus = getLocalFollowStatus(trimmedNickname)
                val updatedResult = if (localFollowStatus != FollowStatus.EMPTY) {
                    // 로컬 상태가 있으면 우선 사용
                    Timber.d("FriendView: 로컬 팔로우 상태 적용 - $trimmedNickname: $localFollowStatus")
                    result.copy(followStatus = localFollowStatus)
                } else {
                    result
                }
                _searchUiState.value = SearchUiState.Success(updatedResult)
            } catch (e: UserNotFoundException) {
                Timber.Forest.e(e, "사용자를 찾을 수 없음: $trimmedNickname")
                _searchUiState.value = SearchUiState.Error("존재하지 않는 유저입니다")
            } catch (e: Exception) {
                Timber.Forest.e(e, "사용자 검색 실패: $trimmedNickname")
                _searchUiState.value = SearchUiState.Error("검색 중 오류가 발생했습니다")
            }
        }
    }

    fun blockFriend(friendId: String) {
        viewModelScope.launch {
            when (val result = friendRepository.blockUser(friendId)) {
                is Result.Success -> {
                    // 차단 성공 시 목록 새로고침
                    loadFriends()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "친구 차단 실패: $friendId")
                    // 차단 실패 시 로컬에서만 제거 (Optimistic UI)
                    _friends.update { current -> current.filterNot { it.id == friendId } }
                }
                Result.Loading -> {} // 처리 중
            }
        }
    }

    /**
     * 닉네임으로 사용자 팔로우 (Optimistic UI 패턴)
     *
     * 버튼 클릭 시 즉시 UI를 업데이트하고, 서버 요청 실패 시에만 롤백합니다.
     *
     * @param nickname 팔로우할 사용자의 닉네임
     */
    fun followUser(nickname: String) {
        val trimmedNickname = nickname.trim()
        if (trimmedNickname.isBlank() || _isFollowing.value) {
            return
        }

        // 현재 상태 저장 (롤백용)
        val currentState = _searchUiState.value
        val previousResult = (currentState as? SearchUiState.Success)?.result
        val previousFollowStatus = previousResult?.followStatus

        // 이미 팔로우 중이거나 팔로잉 상태면 처리하지 않음
        if (previousFollowStatus == FollowStatus.ACCEPTED ||
            previousFollowStatus == FollowStatus.PENDING) {
            return
        }

        // Optimistic UI: 즉시 UI 업데이트 (PENDING 상태로 변경)
        _searchUiState.update { state ->
            when (state) {
                is SearchUiState.Success -> {
                    SearchUiState.Success(
                        result = state.result.copy(
                            followStatus = FollowStatus.PENDING
                        )
                    )
                }
                else -> state
            }
        }

        // 낙관적 업데이트 시점에는 로컬 저장하지 않음 (서버 성공 시에만 저장)

        // 버튼 비활성화
        _isFollowing.value = true

        // 서버 요청 (백그라운드)
        viewModelScope.launch {
            try {
                followRemoteDataSource.followUserByNickname(trimmedNickname)
                Timber.d("팔로우 성공: $trimmedNickname")
                // 성공 시 로컬에 PENDING 상태 저장 (FriendSearchDetail 동기화)
                saveFollowStatusToLocal(trimmedNickname, FollowStatus.PENDING)
                Timber.d("FriendViewModel.followUser: saved PENDING status to local for $trimmedNickname")
            } catch (e: FollowUserNotFoundException) {
                Timber.e(e, "존재하지 않는 유저: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousResult, previousFollowStatus)
            } catch (e: FollowSelfException) {
                Timber.e(e, "자기 자신에게 팔로우: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousResult, previousFollowStatus)
            } catch (e: FollowRequestAlreadyExistsException) {
                Timber.e(e, "이미 보낸 팔로우 신청: $trimmedNickname")
                // 이미 PENDING 상태이므로 롤백 불필요 (상태 유지)
            } catch (e: AlreadyFollowingException) {
                Timber.e(e, "이미 팔로우 중: $trimmedNickname")
                // 이미 팔로우 중이므로 ACCEPTED 상태로 변경
                _searchUiState.update { state ->
                    when (state) {
                        is SearchUiState.Success -> {
                            SearchUiState.Success(
                                result = state.result.copy(
                                    followStatus = FollowStatus.ACCEPTED
                                )
                            )
                        }
                        else -> state
                    }
                }
                saveFollowStatusToLocal(trimmedNickname, FollowStatus.ACCEPTED)
            } catch (e: Exception) {
                Timber.e(e, "팔로우 실패: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousResult, previousFollowStatus)
            } finally {
                _isFollowing.value = false
            }
        }
    }

    /**
     * 팔로우 상태 롤백
     */
    private fun rollbackFollowStatus(
        previousResult: UserSearchResult?,
        previousFollowStatus: FollowStatus?,
    ) {
        if (previousResult != null && previousFollowStatus != null) {
            _searchUiState.update { state ->
                when (state) {
                    is SearchUiState.Success -> {
                        SearchUiState.Success(
                            result = previousResult.copy(
                                followStatus = previousFollowStatus
                            )
                        )
                    }
                    else -> state
                }
            }
        }
    }

    /**
     * 로컬 SharedPreferences에서 팔로우 상태 확인
     */
    private fun getLocalFollowStatus(nickname: String): FollowStatus {
        return try {
            val statusString = followPrefs.getString("follow_status_$nickname", null)
            statusString?.let { FollowStatus.valueOf(it) } ?: FollowStatus.EMPTY
        } catch (e: Exception) {
            Timber.e(e, "로컬 팔로우 상태 로드 실패: $nickname")
            FollowStatus.EMPTY
        }
    }

    /**
     * 팔로우 상태를 로컬에 저장 (ViewModel 간 공유용)
     */
    private fun saveFollowStatusToLocal(nickname: String, status: FollowStatus) {
        try {
            followPrefs.edit()
                .putString("follow_status_$nickname", status.name)
                .apply()
            Timber.d("FriendView: 팔로우 상태 로컬 저장 - $nickname: $status, key=follow_status_$nickname")
        } catch (e: Exception) {
            Timber.e(e, "팔로우 상태 로컬 저장 실패: $nickname")
        }
    }
}