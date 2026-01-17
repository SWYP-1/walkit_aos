package swyp.team.walkit.ui.friend

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
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.exception.AuthExpiredException
import swyp.team.walkit.data.remote.friend.FollowRemoteDataSource
import swyp.team.walkit.data.remote.user.AlreadyFollowingException
import swyp.team.walkit.data.remote.user.FollowRequestAlreadyExistsException
import swyp.team.walkit.data.remote.user.FollowSelfException
import swyp.team.walkit.data.remote.user.FollowUserNotFoundException
import swyp.team.walkit.data.remote.user.UserNotFoundException
import swyp.team.walkit.data.remote.user.UserRemoteDataSource
import swyp.team.walkit.data.remote.user.UserSearchResult
import swyp.team.walkit.domain.model.FollowStatus
import swyp.team.walkit.domain.model.Friend
import swyp.team.walkit.core.AuthEventBus
import swyp.team.walkit.domain.repository.FriendRepository
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
    data class Success(val results: List<UserSearchResult>) : SearchUiState
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
    private val authEventBus: AuthEventBus,
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
        application.getSharedPreferences(
            "follow_status_prefs",
            android.content.Context.MODE_PRIVATE
        )
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

                    // 토큰 만료 에러인지 확인
                    if (result.exception is AuthExpiredException) {
                        Timber.w("토큰 만료로 인한 친구 목록 로드 실패 - 로그인 필요")
                        // 로그인 화면으로 이동하는 이벤트 발생
                        authEventBus.notifyRequireLogin()
                    }

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
                val results = userRemoteDataSource.searchUserByNickname(trimmedNickname)
                if (results.isEmpty()) {
                    _searchUiState.value = SearchUiState.Error("검색 결과를 찾을 수 없습니다")
                    return@launch
                }

                // 여러 결과 중 첫 번째 결과 사용 (또는 정확히 일치하는 결과 우선)
                val result =
                    results.firstOrNull { it.nickname.equals(trimmedNickname, ignoreCase = true) }
                        ?: results.first()

                // API 응답을 우선 사용하되, 로컬 상태와 동기화
                // API에서 EMPTY가 왔을 때는 로컬 상태를 무시 (실제 상태가 변경되었을 수 있음)
                val finalResult = when (result.followStatus) {
                    FollowStatus.EMPTY -> {
                        // API에서 EMPTY가 왔으므로 로컬 상태 초기화 및 API 상태 사용
                        clearLocalFollowStatus(trimmedNickname)
                        Timber.d("FriendView: API에서 EMPTY 수신 - 로컬 상태 초기화 및 API 상태 사용: $trimmedNickname")
                        result
                    }

                    FollowStatus.PENDING, FollowStatus.ACCEPTED -> {
                        // API에서 실제 팔로우 상태가 왔으므로 로컬과 동기화
                        saveFollowStatusToLocal(trimmedNickname, result.followStatus)
                        Timber.d("FriendView: API 팔로우 상태 적용 및 로컬 동기화 - $trimmedNickname: ${result.followStatus}")
                        result
                    }

                    else -> {
                        // 다른 상태는 API 응답 사용
                        Timber.d("FriendView: API 상태 사용 - $trimmedNickname: ${result.followStatus}")
                        result
                    }
                }
                _searchUiState.value = SearchUiState.Success(listOf(finalResult))
            } catch (e: UserNotFoundException) {
                Timber.Forest.e(e, "사용자를 찾을 수 없음: $trimmedNickname")
                _searchUiState.value = SearchUiState.Error("존재하지 않는 유저입니다")
            } catch (t: Throwable) {
                Timber.Forest.e(t, "사용자 검색 실패: $trimmedNickname")
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
        val previousResults = (currentState as? SearchUiState.Success)?.results
        val previousFollowStatus =
            previousResults?.find { it.nickname == trimmedNickname }?.followStatus

        // 이미 팔로우 중이거나 팔로잉 상태면 처리하지 않음
        if (previousFollowStatus == FollowStatus.ACCEPTED ||
            previousFollowStatus == FollowStatus.PENDING
        ) {
            return
        }

        // Optimistic UI: 즉시 UI 업데이트 (PENDING 상태로 변경)
        _searchUiState.update { state ->
            when (state) {
                is SearchUiState.Success -> {
                    SearchUiState.Success(
                        results = state.results.map { result ->
                            if (result.nickname == trimmedNickname) {
                                result.copy(followStatus = FollowStatus.PENDING)
                            } else {
                                result
                            }
                        }
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
                rollbackFollowStatus(previousResults, trimmedNickname, previousFollowStatus)
            } catch (e: FollowSelfException) {
                Timber.e(e, "자기 자신에게 팔로우: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousResults, trimmedNickname, previousFollowStatus)
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
                                results = state.results.map { result ->
                                    if (result.nickname == trimmedNickname) {
                                        result.copy(followStatus = FollowStatus.ACCEPTED)
                                    } else {
                                        result
                                    }
                                }
                            )
                        }

                        else -> state
                    }
                }
                saveFollowStatusToLocal(trimmedNickname, FollowStatus.ACCEPTED)
            } catch (t: Throwable) {
                Timber.e(t, "팔로우 실패: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousResults, trimmedNickname, previousFollowStatus)
            } finally {
                _isFollowing.value = false
            }
        }
    }

    /**
     * 팔로우 상태 롤백
     */
    private fun rollbackFollowStatus(
        previousResults: List<UserSearchResult>?,
        nickname: String,
        previousFollowStatus: FollowStatus?,
    ) {
        if (previousResults != null && previousFollowStatus != null) {
            _searchUiState.update { state ->
                when (state) {
                    is SearchUiState.Success -> {
                        SearchUiState.Success(
                            results = state.results.map { result ->
                                if (result.nickname == nickname) {
                                    result.copy(followStatus = previousFollowStatus)
                                } else {
                                    result
                                }
                            }
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
        } catch (t: Throwable) {
            Timber.e(t, "로컬 팔로우 상태 로드 실패: $nickname")
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
        } catch (t: Throwable) {
            Timber.e(t, "팔로우 상태 로컬 저장 실패: $nickname")
        }
    }

    /**
     * 로컬 팔로우 상태 초기화 (잘못된 상태 동기화를 방지)
     */
    private fun clearLocalFollowStatus(nickname: String) {
        try {
            followPrefs.edit()
                .remove("follow_status_$nickname")
                .apply()
            Timber.d("FriendView: 로컬 팔로우 상태 초기화 - $nickname")
        } catch (t: Throwable) {
            Timber.e(t, "로컬 팔로우 상태 초기화 실패: $nickname")
        }
    }
}