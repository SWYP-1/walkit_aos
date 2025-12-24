package team.swyp.sdu.ui.record.friendrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.walking.WalkRemoteDataSource
import team.swyp.sdu.data.remote.walking.mapper.FollowerWalkRecordMapper
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.model.FollowerWalkRecord
import timber.log.Timber
import java.util.LinkedHashMap
import javax.inject.Inject

/**
 * LRU 캐시 최대 크기
 */
private const val MAX_CACHE_SIZE = 5

/**
 * 친구별 기록 상태
 */
data class FriendRecordState(
    val recordsResult: Result<List<FollowerWalkRecord>> = Result.Loading,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
)

/**
 * 친구 기록 리스트 화면 ViewModel
 *
 * LRU 캐시를 사용하여 친구별 상태를 관리합니다.
 * 최근 사용한 친구 상태 N개만 유지하고, 캐시가 초과되면 가장 오래 사용되지 않은 친구 상태를 제거합니다.
 */
@HiltViewModel
class FriendRecordListViewModel
    @Inject
    constructor(
        private val walkRemoteDataSource: WalkRemoteDataSource,
    ) : ViewModel() {
        // 친구 목록 (자체 관리)
        // TODO: 실제 API가 구현되면 UserRemoteDataSource나 FriendRepository를 통해 가져오도록 수정
        private val _friends = MutableStateFlow<List<Friend>>(
            listOf(
                Friend("1", "닉네임"),
                Friend("2", "네이버스두"),
                Friend("3", "네이버스두"),
                Friend("4", "닉네임04"),
                Friend("5", "닉네임05"),
                Friend("6", "닉네임06"),
            )
        )
        val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

        // 선택된 친구 ID
        private val _selectedFriendId = MutableStateFlow<String?>(null)
        val selectedFriendId: StateFlow<String?> = _selectedFriendId.asStateFlow()

        // LRU 캐시: LinkedHashMap을 access-order로 사용
        // accessOrder = true: get/put 시 접근 순서가 유지됨 (가장 오래된 항목이 앞에)
        private val friendStateCache: LinkedHashMap<String, FriendRecordState> =
            object : LinkedHashMap<String, FriendRecordState>(MAX_CACHE_SIZE + 1, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FriendRecordState>?): Boolean {
                    return size > MAX_CACHE_SIZE
                }
            }

        // 친구별 상태 (UI에 노출)
        private val _friendStates = MutableStateFlow<Map<String, FriendRecordState>>(emptyMap())
        val friendStates: StateFlow<Map<String, FriendRecordState>> = _friendStates.asStateFlow()

        init {
            // 첫 번째 친구를 기본 선택
            viewModelScope.launch {
                friends.collect { friendList ->
                    if (friendList.isNotEmpty() && _selectedFriendId.value == null) {
                        selectFriend(friendList.first().id)
                    }
                }
            }
        }

        /**
         * 친구 선택
         *
         * 캐시에 상태가 있으면 즉시 표시하고, 없으면 새로 로딩합니다.
         */
        fun selectFriend(friendId: String) {
            if (_selectedFriendId.value == friendId) {
                return // 이미 선택된 친구
            }

            _selectedFriendId.value = friendId

            // 캐시에서 상태 확인
            val cachedState = friendStateCache[friendId]
            if (cachedState != null) {
                // 캐시에 있으면 즉시 표시 (네트워크 재호출 없음)
                Timber.d("캐시에서 친구 상태 복원: $friendId")
                updateFriendState(friendId, cachedState)
            } else {
                // 캐시에 없으면 새로 로딩
                Timber.d("새로운 친구 기록 로딩: $friendId")
                loadFriendRecords(friendId)
            }
        }

        /**
         * 친구 기록 로딩
         */
        private fun loadFriendRecords(friendId: String) {
            val friend = friends.value.find { it.id == friendId } ?: return

            // 로딩 상태로 초기화
            val loadingState = FriendRecordState(recordsResult = Result.Loading)
            updateFriendState(friendId, loadingState)

            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val result = walkRemoteDataSource.getFollowerWalkRecord(friend.nickname)
                        
                        when (result) {
                            is Result.Success -> {
                                // DTO를 Domain 모델로 변환
                                val domainRecord = FollowerWalkRecordMapper.toDomain(result.data)
                                val recordsList = listOf(domainRecord)
                                val successState = FriendRecordState(
                                    recordsResult = Result.Success(recordsList)
                                )
                                updateFriendState(friendId, successState)
                            }
                            is Result.Error -> {
                                val errorState = FriendRecordState(
                                    recordsResult = Result.Error(result.exception, result.message)
                                )
                                updateFriendState(friendId, errorState)
                            }
                            is Result.Loading -> {
                                // 이미 로딩 상태이므로 변경 없음
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "친구 기록 로딩 실패: $friendId")
                    val errorState = FriendRecordState(
                        recordsResult = Result.Error(e, e.message ?: "알 수 없는 오류")
                    )
                    updateFriendState(friendId, errorState)
                }
            }
        }

        /**
         * 친구 상태 업데이트 및 캐시 저장
         */
        private fun updateFriendState(friendId: String, state: FriendRecordState) {
            // 캐시에 저장 (access-order로 인해 자동으로 최신 항목이 뒤로 이동)
            friendStateCache[friendId] = state

            // UI 상태 업데이트
            _friendStates.value = friendStateCache.toMap()
        }

        /**
         * 스크롤 위치 저장
         */
        fun saveScrollPosition(friendId: String, index: Int, offset: Int) {
            val currentState = friendStateCache[friendId] ?: return
            val updatedState = currentState.copy(
                scrollIndex = index,
                scrollOffset = offset
            )
            updateFriendState(friendId, updatedState)
        }
    }


