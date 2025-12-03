package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.api.ApiService
import team.swyp.sdu.data.api.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PostViewModel
    @Inject
    constructor(
        private val apiService: ApiService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
        val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

        init {
            // 초기 로딩은 필요시 수동으로 호출
            // loadPosts()
        }

        fun loadPosts() {
            viewModelScope.launch {
                Timber.d("포스트 로딩 시작")
                _uiState.value = PostUiState.Loading
                try {
                    val posts = apiService.getPosts()
                    Timber.d("포스트 로딩 성공: ${posts.size}개")
                    _uiState.value = PostUiState.Success(posts)
                } catch (e: Exception) {
                    Timber.e(e, "포스트 로딩 실패")
                    _uiState.value = PostUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다")
                }
            }
        }

        fun loadPostById(id: Int) {
            viewModelScope.launch {
                Timber.d("포스트 ID $id 로딩 시작")
                _uiState.value = PostUiState.Loading
                try {
                    val post = apiService.getPost(id)
                    Timber.d("포스트 ID $id 로딩 성공: ${post.title}")
                    _uiState.value = PostUiState.Success(listOf(post))
                } catch (e: Exception) {
                    Timber.e(e, "포스트 ID $id 로딩 실패")
                    _uiState.value = PostUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다")
                }
            }
        }
    }

sealed interface PostUiState {
    data object Loading : PostUiState

    data class Success(
        val posts: List<Post>,
    ) : PostUiState

    data class Error(
        val message: String,
    ) : PostUiState
}
