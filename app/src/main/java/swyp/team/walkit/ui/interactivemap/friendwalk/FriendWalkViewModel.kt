package swyp.team.walkit.ui.interactivemap.friendwalk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.FollowerLatestWalkRecord
import swyp.team.walkit.domain.repository.FollowerMapRepository
import swyp.team.walkit.domain.repository.WalkRepository
import swyp.team.walkit.ui.record.friendrecord.LikeUiState
import timber.log.Timber
import javax.inject.Inject

private const val LIKE_DEBOUNCE_MS = 500L

/**
 * 친구 산책 화면 UI 상태
 */
data class FriendWalkUiState(
    val isLoading: Boolean = true,
    val walkRecord: FollowerLatestWalkRecord? = null,
    val likeState: LikeUiState = LikeUiState(),
    val errorMessage: String? = null,
)

/**
 * 친구 산책 화면 ViewModel
 *
 * nav args로 전달받은 userId를 사용하여 최근 산책 기록을 로드한다.
 */
@HiltViewModel
class FriendWalkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val followerMapRepository: FollowerMapRepository,
    private val walkRepository: WalkRepository,
) : ViewModel() {

    private val userId: Long = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(FriendWalkUiState())
    val uiState: StateFlow<FriendWalkUiState> = _uiState.asStateFlow()

    private var likeToggleJob: Job? = null

    init {
        loadWalkRecord()
    }

    /**
     * 팔로워 최근 산책 기록 로드
     */
    private fun loadWalkRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = followerMapRepository.getFollowerLatestWalkRecord(userId)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        walkRecord = result.data,
                        likeState = LikeUiState(
                            count = result.data.likeCount,
                            isLiked = result.data.liked,
                        ),
                    )
                }
                is Result.Error -> {
                    Timber.e(result.exception, "친구 산책 기록 로드 실패: userId=$userId")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    /**
     * 좋아요 토글 — Optimistic UI + debounce + 실패 시 롤백
     */
    fun toggleLike() {
        val currentLike = _uiState.value.likeState
        val isCurrentlyLiked = currentLike.isLiked

        val optimistic = currentLike.copy(
            isLiked = !isCurrentlyLiked,
            count = if (isCurrentlyLiked) (currentLike.count - 1).coerceAtLeast(0)
                    else currentLike.count + 1,
        )
        _uiState.update { it.copy(likeState = optimistic) }

        val walkId = _uiState.value.walkRecord?.walkId ?: run {
            Timber.w("좋아요 토글 실패: walkRecord가 아직 로드되지 않음")
            return
        }

        likeToggleJob?.cancel()
        likeToggleJob = viewModelScope.launch {
            delay(LIKE_DEBOUNCE_MS)
            val result = withContext(Dispatchers.IO) {
                if (isCurrentlyLiked) walkRepository.unlikeWalk(walkId)
                else walkRepository.likeWalk(walkId)
            }
            if (result is Result.Error) {
                Timber.e(result.exception, "좋아요 토글 실패, 롤백: walkId=$walkId")
                _uiState.update { it.copy(likeState = currentLike) }
            }
        }
    }
}
