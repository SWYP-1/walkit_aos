package team.swyp.sdu.ui.mypage.userInfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Sex
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.repository.GoalRepository
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.*
import timber.log.Timber
import javax.inject.Inject

/**
 * 사용자 정보 관리 ViewModel
 */
@HiltViewModel
class UserInfoManagementViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<UserInfoUiState>(UserInfoUiState.Loading)
    val uiState: StateFlow<UserInfoUiState> = _uiState.asStateFlow()

    // 사용자 정보 입력 상태
    private val _userInput = MutableStateFlow(UserInput())
    val userInput: StateFlow<UserInput> = _userInput.asStateFlow()

    // Goal 상태
    val goalFlow: StateFlow<Goal?> = goalRepository.goalFlow

    val _uploadedImageUri = MutableStateFlow(Uri.EMPTY)
    val uploadedImageUri: StateFlow<Uri> = _uploadedImageUri.asStateFlow()

    init {
        loadUserInfo()
        loadGoal()
    }

    /**
     * Goal 조회
     */
    private fun loadGoal() {
        viewModelScope.launch {
            goalRepository.getGoal()
        }
    }

    /**
     * 사용자 정보 조회
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = UserInfoUiState.Loading

            when (val result = userRepository.getUser()) {
                is Result.Success -> {
                    val user = result.data
                    _userInput.value = UserInput(
                        nickname = user.nickname ?: "",
                        birthDate = user.birthDate ?: "",
                        sex = user.sex ?: Sex.MALE,
                        imageName = user.imageName,
                    )
                    _uiState.value = Success(user)
                }

                is Result.Error -> {
                    Timber.e(result.exception, "사용자 정보 조회 실패")
                    _uiState.value = Error(result.message ?: "사용자 정보 조회에 실패했습니다")
                }

                is Result.Loading -> {

                }

            }
        }
    }

    /**
     * 사용자 입력 상태 업데이트
     */
    fun updateUserInput(input: UserInput) {
        _userInput.value = input
    }
    /**
     * 사용자 입력 상태 업데이트
     */
    fun updateProfileImageUri(uri: Uri) {
        _uploadedImageUri.value = uri
    }

    /**
     * _uploadedImageUri를 사용하여 프로필 저장/업데이트
     *
     * - _uploadedImageUri가 Uri.EMPTY이면 이미지 없이 프로필만 저장
     * - _uploadedImageUri가 Uri.EMPTY가 아니면 이미지 업데이트 포함
     */
    fun saveUserProfile(
        birthYear: String,
        birthMonth: String,
        birthDay: String,
        nickname: String,
    ) {
        viewModelScope.launch {
            _uiState.value = UserInfoUiState.Updating

            try {
                // 생년월일 포맷팅
                val birthDate =
                    if (birthYear.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank()) {
                        String.format(
                            "%04d-%02d-%02d",
                            birthYear.toIntOrNull() ?: 2000,
                            birthMonth.toIntOrNull() ?: 1,
                            birthDay.toIntOrNull() ?: 1
                        )
                    } else ""

                // 입력값 검증
                if (nickname.isBlank()) {
                    _uiState.value = UserInfoUiState.Error("닉네임을 입력해주세요")
                    return@launch
                }
                if (birthDate.isBlank()) {
                    _uiState.value = UserInfoUiState.Error("생년월일을 선택해주세요")
                    return@launch
                }

                val currentImageUri = _uploadedImageUri.value

                // _uploadedImageUri가 Uri.EMPTY인지 확인
                val hasImage = currentImageUri != Uri.EMPTY

                // 1️⃣ 병렬 실행
                val imageDeferred = async {
                    if (hasImage) {
                        // 이미지가 있으면 업데이트
                        userRepository.updateUserProfileImage(currentImageUri)
                    } else {
                        // 이미지가 없으면 성공 처리 (이미지 없이 저장)
                        Result.Success(Unit)
                    }
                }

                val profileDeferred = async {
                    userRepository.updateUserProfile(
                        nickname = nickname,
                        birthDate = birthDate
                    )
                }

                val imageResult = imageDeferred.await()
                val profileResult = profileDeferred.await()

                // 2️⃣ 결과 합산
                when {
                    imageResult is Result.Error -> {
                        _uiState.value =
                            UserInfoUiState.Error(imageResult.message ?: "이미지 업데이트 실패")
                        return@launch
                    }

                    profileResult is Result.Error -> {
                        _uiState.value =
                            UserInfoUiState.Error(profileResult.message ?: "프로필 업데이트 실패")
                        return@launch
                    }

                    imageResult is Result.Success && profileResult is Result.Success -> {
                        // ✅ 둘 다 성공한 후 최신 데이터를 가져와서 Room 업데이트
                        val refreshResult = userRepository.refreshUser()
                        when (refreshResult) {
                            is Result.Success -> {
                                val updatedUser = refreshResult.data
                                _userInput.value = UserInput(
                                    nickname = updatedUser.nickname ?: "",
                                    birthDate = updatedUser.birthDate ?: "",
                                    sex = updatedUser.sex ?: Sex.MALE,
                                    imageName = updatedUser.imageName,
                                    selectedImageUri = null // 저장 성공 시 선택된 이미지 URI 초기화
                                )
                                // 성공 시 _uploadedImageUri 초기화
                                _uploadedImageUri.value = Uri.EMPTY
                                _uiState.value = Success(updatedUser)
                                Timber.d("프로필 저장 완료: nickname=$nickname, hasImage=$hasImage, imageName=${updatedUser.imageName}")
                            }
                            is Result.Error -> {
                                _uiState.value = Error("프로필 데이터 동기화 실패")
                                Timber.e("프로필 저장 후 데이터 동기화 실패")
                            }

                            Result.Loading -> {}
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "프로필 저장 중 예외 발생")
                _uiState.value = UserInfoUiState.Error("프로필 저장에 실패했습니다")
            }
        }
    }

}

/**
 * UI 상태
 */
sealed interface UserInfoUiState {
    data object Loading : UserInfoUiState
    data object Updating : UserInfoUiState
    data class Success(val user: User) : UserInfoUiState
    data class Error(val message: String) : UserInfoUiState
}

/**
 * 사용자 입력 데이터
 */
data class UserInput(
    val name: String = "",
    val nickname: String = "",
    val birthDate: String = "",
    val sex: Sex = Sex.MALE,
    val imageName: String? = null,
    val selectedImageUri: String? = null,
)


