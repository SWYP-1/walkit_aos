package team.swyp.sdu.ui.mypage.userInfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.repository.GoalRepository
import team.swyp.sdu.data.local.datastore.AuthDataStore
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.*
import team.swyp.sdu.ui.mypage.userInfo.UserInput
import timber.log.Timber
import javax.inject.Inject

/**
 * 사용자 정보 관리 ViewModel
 */
@HiltViewModel
class UserInfoManagementViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
    private val authDataStore: AuthDataStore,
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<UserInfoUiState>(UserInfoUiState.Loading)
    val uiState: StateFlow<UserInfoUiState> = _uiState.asStateFlow()

    // 사용자 정보 입력 상태
    private val _userInput = MutableStateFlow(UserInput())
    val userInput: StateFlow<UserInput> = _userInput.asStateFlow()

    // Goal 상태
    val goalFlow: StateFlow<Goal?> = goalRepository.goalFlow

    // 연동된 계정 정보 (Flow를 StateFlow로 변환)
    val provider: StateFlow<String?> = authDataStore.provider
        .map { providerValue ->
            when (providerValue) {
                "kakao" -> "카카오"
                "naver" -> "네이버"
                else -> providerValue
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val _uploadedImageUri = MutableStateFlow(Uri.EMPTY)
    val uploadedImageUri: StateFlow<Uri> = _uploadedImageUri.asStateFlow()

    // 프로필 이미지 삭제 상태
    private val _imageDeleted = MutableStateFlow(false)
    val imageDeleted: StateFlow<Boolean> = _imageDeleted.asStateFlow()

    // 변경사항 추적 상태
    private val _hasChange = MutableStateFlow(false)
    val hasChange: StateFlow<Boolean> = _hasChange.asStateFlow()

    // 현재 사용자 데이터 (UI 상태 복원을 위해)
    private var currentUser: User? = null


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
                    currentUser = user // 현재 사용자 데이터 저장
                    _userInput.value = UserInput(
                        nickname = user.nickname ?: "",
                        birthDate = user.birthDate ?: "",
                        email = user.email,
                        imageName = user.imageName,
                    )
                    _hasChange.value = false // 초기 로딩 시 변경사항 없음
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
        Timber.d("updateUserInput: nickname=${input.nickname}, isDuplicate=${input.isNicknameDuplicate}, error=${input.nicknameValidationError}")
        _userInput.value = input  // update 대신 직접 할당
        _hasChange.value = true
        Timber.d("userInput updated successfully")
    }
    /**
     * 사용자 입력 상태 업데이트
     * uri가 null이면 이미지 삭제
     */
    fun updateProfileImageUri(uri: Uri?) {
        if (uri == null) {
            // 이미지 삭제
            _imageDeleted.value = true
            _uploadedImageUri.value = Uri.EMPTY
        } else {
            // 새 이미지 설정
            _imageDeleted.value = false
            _uploadedImageUri.value = uri
        }
        _hasChange.value = true // 이미지 변경 시 변경사항 표시
    }

    /**
     * 프로필 이미지 삭제
     * 성공 여부는 로그만 남김
     */
    private suspend fun deleteProfileImage() {
        try {
            // 현재 사용자 정보에서 imageName을 imageId로 사용 (Long 변환 시도)
            val currentUser = when (val uiState = _uiState.value) {
                is UserInfoUiState.Success -> uiState.user
                else -> null
            }

            val result = userRepository.deleteImage()
            when (result) {
                is Result.Success -> {
                    if (result.data.isSuccessful) {
                        Timber.d("프로필 이미지 삭제 성공: ")
                    } else {
                        Timber.e("프로필 이미지 삭제 실패: HTTP ${result.data.code()}")
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "프로필 이미지 삭제 실패: ${result.message}")
                }
                Result.Loading -> {}
            }
        } catch (e: Exception) {
            Timber.e(e, "프로필 이미지 삭제 중 예외 발생")
        }
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

                Timber.d("프로필 업데이트 시도: nickname=$nickname, birthDate=$birthDate")

                val currentImageUri = _uploadedImageUri.value
                val shouldDeleteImage = _imageDeleted.value

                // 2️⃣ 닉네임 등록 성공 시에만 다른 필드들 업데이트
                val imageDeferred = async {
                    when {
                        shouldDeleteImage -> {
                            Timber.d("프로필 이미지 삭제 시도")
                            deleteProfileImage()
                            Result.Success(Unit)
                        }
                        currentImageUri != Uri.EMPTY -> {
                            Timber.d("새 프로필 이미지 업로드 시도")
                            userRepository.updateUserProfileImage(currentImageUri)
                        }
                        else -> {
                            Timber.d("프로필 이미지 변경 없음")
                            Result.Success(Unit)
                        }
                    }
                }

                val profileDeferred = async {
                    Timber.d("프로필 정보 업데이트 시도: birthDate=$birthDate")
                    userRepository.updateUserProfile(
                        nickname = nickname, // 이미 등록된 닉네임
                        birthDate = birthDate
                    )
                }

                val imageResult = imageDeferred.await()
                val profileResult = profileDeferred.await()

                // 3️⃣ 결과 처리
                when {
                    imageResult is Result.Error -> {
                        _uiState.value = UserInfoUiState.Error(imageResult.message ?: "이미지 업데이트 실패")
                        return@launch
                    }

                    profileResult is Result.Error -> {
                        // 프로필 업데이트 실패 시 닉네임 중복 에러 처리
                        Timber.e(profileResult.exception, "프로필 업데이트 실패: ${profileResult.message}")

                        // Onboarding과 동일한 에러 처리 방식
                        when (profileResult.exception?.message) {
                            "NICKNAME_VALIDATION_ERROR" -> {
                                // 닉네임 규칙 위반
                                _userInput.value = _userInput.value.copy(
                                    isNicknameDuplicate = null,
                                    nicknameValidationError = "닉네임 형식이 올바르지 않습니다."
                                )
                            }
                            "DUPLICATE_NICKNAME" -> {
                                // 닉네임 중복
                                _userInput.value = _userInput.value.copy(
                                    isNicknameDuplicate = true,
                                    nicknameValidationError = "중복된 닉네임입니다."
                                )
                            }
                            else -> {
                                // 기타 에러
                                _uiState.value = UserInfoUiState.Error(profileResult.message ?: "프로필 업데이트 실패")
                                return@launch
                            }
                        }

                        // 닉네임 에러의 경우 UI 상태를 복원하고 계속 진행
                        currentUser?.let { user ->
                            _uiState.value = UserInfoUiState.Success(user)
                        }
                        return@launch
                    }

                    imageResult is Result.Success && profileResult is Result.Success -> {
                        Timber.d("모든 업데이트 성공, 최신 데이터 동기화 시도")
                        // ✅ 모든 업데이트 성공 시 최신 데이터를 가져와서 UI 업데이트
                        val refreshResult = userRepository.refreshUser()
                        when (refreshResult) {
                            is Result.Success -> {
                                val updatedUser = refreshResult.data
                                _userInput.value = UserInput(
                                    nickname = updatedUser.nickname ?: "",
                                    birthDate = updatedUser.birthDate ?: "",
                                    email = updatedUser.email,
                                    imageName = updatedUser.imageName,
                                    selectedImageUri = null, // 저장 성공 시 선택된 이미지 URI 초기화
                                    isNicknameDuplicate = false, // 저장 성공 시 중복 상태 초기화
                                    nicknameValidationError = null // 에러 상태 초기화
                                )
                                // 성공 시 상태 초기화
                                _uploadedImageUri.value = Uri.EMPTY
                                _imageDeleted.value = false
                                _hasChange.value = false // 저장 성공 시 변경사항 리셋
                                _uiState.value = Success(updatedUser)
                                Timber.d("프로필 저장 완료: nickname=$nickname, birthDate=$birthDate, imageName=${updatedUser.imageName}")
                            }
                            is Result.Error -> {
                                _uiState.value = Error("프로필 데이터 동기화 실패")
                                Timber.e("프로필 저장 후 데이터 동기화 실패: ${refreshResult.message}")
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


    /**
     * 프로필 이미지 삭제
     */
    suspend fun deleteImage(): Result<Response<Unit>> {
        return try {
            userRepository.deleteImage()
        } catch (e: Exception) {
            Timber.e(e, "프로필 이미지 삭제 중 예외 발생")
            Result.Error(e, e.message ?: "프로필 이미지 삭제에 실패했습니다")
        }
    }

}

/**
 * UI 상태
 */
sealed interface UserInfoUiState {
    data object Loading : UserInfoUiState
    data object Updating : UserInfoUiState
    data object CheckingDuplicate : UserInfoUiState
    data class Success(val user: User) : UserInfoUiState
    data class Error(val message: String) : UserInfoUiState
}



