package team.swyp.sdu.ui.friend

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
import team.swyp.sdu.data.remote.walking.mapper.FollowerWalkRecordMapper
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.repository.WalkRepository
import team.swyp.sdu.domain.service.LocationManager
import team.swyp.sdu.utils.LocationConstants
import timber.log.Timber
import javax.inject.Inject

/**
 * 친구 검색 상세 화면 ViewModel
 */
@HiltViewModel
class FriendSearchViewModel
@Inject
constructor(
    private val walkRepository: WalkRepository,
    private val userRepository: UserRepository,
    private val locationManager: LocationManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FriendSearchUiState>(FriendSearchUiState.Loading)
    val uiState: StateFlow<FriendSearchUiState> = _uiState.asStateFlow()

    /**
     * 팔로워 산책 기록 조회
     *
     * @param nickname 팔로워 닉네임 (null이면 내 최근 정보 조회)
     * @param lat 위도 (선택사항)
     * @param lon 경도 (선택사항)
     */
    fun loadFollowerWalkRecord(
        nickname: String,
    ) {
        viewModelScope.launch {
            _uiState.value = FriendSearchUiState.Loading

            // 3️⃣ 현재 위치 가져오기
            val currentLocation = try {
                locationManager.getCurrentLocationOrLast()
            } catch (e: Exception) {
                Timber.w(e, "현재 위치를 가져올 수 없음 - 서울 시청 좌표 사용")
                null
            }

            withContext(Dispatchers.IO) {
                val result = userRepository.getUserSummaryByNickname(
                    nickname = nickname,
                    lat = currentLocation?.latitude ?: LocationConstants.DEFAULT_LATITUDE,
                    lon = currentLocation?.longitude ?: LocationConstants.DEFAULT_LONGITUDE
                )

                _uiState.value = when (result) {
                    is Result.Success -> {
                        val data = result.data
                        FriendSearchUiState.Success(data = data)
                    }

                    is Result.Error -> FriendSearchUiState.Error(result.message)
                    Result.Loading -> FriendSearchUiState.Loading
                }
            }
        }
    }

}


