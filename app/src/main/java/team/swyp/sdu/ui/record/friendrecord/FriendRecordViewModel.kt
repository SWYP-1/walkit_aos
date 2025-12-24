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
import team.swyp.sdu.data.remote.walking.WalkRemoteDataSource
import team.swyp.sdu.data.remote.walking.mapper.FollowerWalkRecordMapper
import team.swyp.sdu.domain.model.FollowerWalkRecord
import timber.log.Timber
import javax.inject.Inject

/**
 * 친구 기록 화면 ViewModel
 */
@HiltViewModel
class FriendRecordViewModel
    @Inject
    constructor(
        private val walkRemoteDataSource: WalkRemoteDataSource,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<FriendRecordUiState>(FriendRecordUiState.Loading)
        val uiState: StateFlow<FriendRecordUiState> = _uiState.asStateFlow()
        
        // Debounce를 위한 Job
        private var likeToggleJob: Job? = null
        private val debounceDelayMs = 500L // 500ms debounce

        /**
         * 팔로워 산책 기록 조회
         *
         * @param nickname 팔로워 닉네임
         */
        fun loadFollowerWalkRecord(nickname: String) {
            viewModelScope.launch {
                _uiState.value = FriendRecordUiState.Loading
                
                withContext(Dispatchers.IO) {
                    val result = walkRemoteDataSource.getFollowerWalkRecord(nickname)
                    _uiState.value = when (result) {
                        is Result.Success -> {
                            val data = FollowerWalkRecordMapper.toDomain(result.data)
                            FriendRecordUiState.Success(
                                data = data,
                                like = LikeUiState.EMPTY, // TODO: API에서 좋아요 정보 가져오기
                            )
                        }
                        is Result.Error -> FriendRecordUiState.Error(result.message)
                        Result.Loading -> FriendRecordUiState.Loading
                    }
                }
            }
        }

        /**
         * 좋아요 토글 (debounce 적용)
         */
        fun toggleLike() {
            val currentState = _uiState.value
            if (currentState !is FriendRecordUiState.Success) {
                return
            }
            
            val walkId = currentState.walkId
            if (walkId == null) {
                Timber.w("walkId가 없어 좋아요 기능을 사용할 수 없습니다")
                return
            }
            
            val isCurrentlyLiked = currentState.like.isLiked
            
            // 즉시 UI 업데이트 (Optimistic Update)
            val newLikeState = if (isCurrentlyLiked) {
                currentState.like.copy(
                    isLiked = false,
                    count = (currentState.like.count - 1).coerceAtLeast(0),
                )
            } else {
                currentState.like.copy(
                    isLiked = true,
                    count = currentState.like.count + 1,
                )
            }
            _uiState.value = currentState.copy(like = newLikeState)
            
            // 이전 Job 취소 (debounce)
            likeToggleJob?.cancel()
            
            // 새로운 Job 시작
            likeToggleJob = viewModelScope.launch {
                delay(debounceDelayMs)
                
                // API 호출
                val result = withContext(Dispatchers.IO) {
                    if (isCurrentlyLiked) {
                        walkRemoteDataSource.unlikeWalk(walkId)
                    } else {
                        walkRemoteDataSource.likeWalk(walkId)
                    }
                }
                
                // API 호출 결과에 따라 UI 상태 업데이트
                when (result) {
                    is Result.Success -> {
                        // 성공 시 이미 업데이트된 상태 유지
                        Timber.d("좋아요 토글 성공: walkId=$walkId, isLiked=${!isCurrentlyLiked}")
                    }
                    is Result.Error -> {
                        // 실패 시 원래 상태로 롤백
                        val rollbackState = currentState.like.copy(
                            isLiked = isCurrentlyLiked,
                            count = if (isCurrentlyLiked) {
                                currentState.like.count
                            } else {
                                (currentState.like.count - 1).coerceAtLeast(0)
                            }
                        )
                        _uiState.value = currentState.copy(like = rollbackState)
                        
                        Timber.w("좋아요 토글 실패: ${result.message}")
                        // TODO: 에러 메시지를 사용자에게 표시 (스낵바 등)
                    }
                    Result.Loading -> {
                        // 로딩 상태는 이미 UI에서 처리됨
                    }
                }
            }
        }
    }


