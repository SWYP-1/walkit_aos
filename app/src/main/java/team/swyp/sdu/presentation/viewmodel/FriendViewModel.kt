package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.swyp.sdu.data.remote.user.UserRemoteDataSource
import team.swyp.sdu.data.remote.user.UserSearchResult
import team.swyp.sdu.domain.model.Friend
import timber.log.Timber

@HiltViewModel
class FriendViewModel
@Inject
constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
) : ViewModel() {

    private val _friends =
        MutableStateFlow(
            listOf(
                Friend("1", "닉네임"),
                Friend("2", "닉네임02"),
                Friend("3", "닉네임03"),
                Friend("4", "닉네임04"),
                Friend("5", "닉네임05"),
                Friend("6", "닉네임06"),
            ),
        )
    val friends: StateFlow<List<Friend>> = _friends

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchResult = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val searchResult: StateFlow<List<UserSearchResult>> = _searchResult

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    init {
        // 검색어 입력이 500ms 동안 멈추면 자동으로 검색 실행
        _query
            .debounce(500L)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .onEach { searchQuery ->
                searchUser(searchQuery)
            }
            .launchIn(viewModelScope)
    }

    val filteredFriends: StateFlow<List<Friend>> =
        combine(_friends, _query) { list, q ->
            val keyword = q.trim()
            if (keyword.isBlank()) list
            else list.filter { friend ->
                friend.nickname.contains(keyword, ignoreCase = true)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = _friends.value,
        )

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
        _searchResult.value = emptyList()
    }

    /**
     * 닉네임으로 사용자 검색
     */
    fun searchUser(nickname: String) {
        val trimmedNickname = nickname.trim()
        if (trimmedNickname.isBlank()) {
            _searchResult.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = userRemoteDataSource.searchUserByNickname(trimmedNickname)
                _searchResult.value = results
            } catch (t: Throwable) {
                Timber.e(t, "사용자 검색 실패: $trimmedNickname")
                _searchResult.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun blockFriend(friendId: String) {
        _friends.update { current -> current.filterNot { it.id == friendId } }
    }
}


