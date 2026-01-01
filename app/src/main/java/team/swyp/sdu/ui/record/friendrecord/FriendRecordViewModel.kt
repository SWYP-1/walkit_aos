package team.swyp.sdu.ui.record.friendrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.walking.mapper.FollowerWalkRecordMapper
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.repository.WalkRepository
import team.swyp.sdu.domain.service.LocationManager
import team.swyp.sdu.utils.LocationConstants
import timber.log.Timber
import java.util.LinkedHashMap
import javax.inject.Inject

private const val MAX_CACHE_SIZE = 5
private const val LIKE_DEBOUNCE_MS = 500L

// 한 팔로워의 산책 기록 하나만 캐시
data class FriendRecordState(
    val record: FollowerWalkRecord
)

@HiltViewModel
class FriendRecordViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val locationManager: LocationManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FriendRecordUiState>(FriendRecordUiState.Loading)
    val uiState: StateFlow<FriendRecordUiState> = _uiState.asStateFlow()

    private var likeToggleJob: Job? = null

    // LRU 캐시: 최근 MAX_CACHE_SIZE명 저장
    private val friendStateCache: LinkedHashMap<String, FriendRecordState> =
        object : LinkedHashMap<String, FriendRecordState>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FriendRecordState>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }

    /**
     * 팔로워 산책 기록 로드
     */
    fun loadFollowerWalkRecord(nickname: String) {
        viewModelScope.launch {
            // 1️⃣ 캐시 확인
            friendStateCache[nickname]?.let { cachedState ->
                _uiState.value = FriendRecordUiState.Success(
                    data = cachedState.record,
                    like = LikeUiState(
                        count = cachedState.record.likeCount,
                        isLiked = cachedState.record.liked
                    )
                )
                return@launch
            }

            // 2️⃣ 로딩 상태
            _uiState.value = FriendRecordUiState.Loading

            // 3️⃣ 현재 위치 가져오기
            val currentLocation = try {
                locationManager.getCurrentLocationOrLast()
            } catch (e: Exception) {
                Timber.w(e, "현재 위치를 가져올 수 없음 - 서울 시청 좌표 사용")
                null
            }

            // 4️⃣ 서버 요청 (위치 정보 포함)
            val result = withContext(Dispatchers.IO) {
                walkRepository.getFollowerWalkRecord(
                    nickname = nickname,
                    lat = currentLocation?.latitude ?: LocationConstants.DEFAULT_LATITUDE,
                    lon = currentLocation?.longitude ?: LocationConstants.DEFAULT_LONGITUDE
                )
            }

            when (result) {
                is Result.Success -> {
                    val record = result.data

                    // 4️⃣ 캐시에 저장 (성공 시)
                    friendStateCache[nickname] = FriendRecordState(record)

                    // 5️⃣ UI 업데이트
                    _uiState.value = FriendRecordUiState.Success(
                        data = record,
                        like = LikeUiState(
                            count = record.likeCount,
                            isLiked = record.liked
                        )
                    )
                }
                is Result.Error -> {
                    // 서버 에러 코드에 따른 구체적인 UI 처리
                    when (result.exception?.message) {
                        "NOT_FOLLOWING" -> {
                            // 팔로워가 아닌 경우
                            _uiState.value = FriendRecordUiState.NotFollowing(
                                message = result.message ?: "팔로우하고 있지 않습니다"
                            )
                        }
                        "NO_WALK_RECORDS" -> {
                            // 산책 기록이 없는 경우
                            _uiState.value = FriendRecordUiState.NoRecords(
                                message = result.message ?: "산책 기록이 아직 없습니다"
                            )
                        }
                        else -> {
                            // 기타 에러
                            _uiState.value = FriendRecordUiState.Error(
                                result.message ?: "데이터를 불러올 수 없습니다"
                            )
                        }
                    }
                }
                Result.Loading -> {} // 이미 Loading 상태
            }
        }
    }

    /**
     * 좋아요 토글 (Optimistic UI + debounce)
     */
    fun toggleLike() {
        val currentState = _uiState.value as? FriendRecordUiState.Success ?: return
        val walkId = currentState.data.walkId
        val isCurrentlyLiked = currentState.like.isLiked

        // 1️⃣ Optimistic UI 업데이트
        _uiState.value = currentState.copy(
            like = currentState.like.copy(
                isLiked = !isCurrentlyLiked,
                count = if (isCurrentlyLiked) (currentState.like.count - 1).coerceAtLeast(0)
                else currentState.like.count + 1
            )
        )

        // 2️⃣ debounce
        likeToggleJob?.cancel()
        likeToggleJob = viewModelScope.launch {
            delay(LIKE_DEBOUNCE_MS)
            val result = withContext(Dispatchers.IO) {
                if (isCurrentlyLiked) walkRepository.unlikeWalk(walkId)
                else walkRepository.likeWalk(walkId)
            }
        }
    }

    fun deleteFriend(nickname: String) {
        friendStateCache.remove(nickname)
    }
}
