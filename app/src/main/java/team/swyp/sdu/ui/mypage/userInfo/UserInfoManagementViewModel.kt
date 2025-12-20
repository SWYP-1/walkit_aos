package team.swyp.sdu.ui.mypage.userInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
     * 사용자 정보 업데이트
     */
    fun updateUserProfile(
        birthYear: String,
        birthMonth: String,
        birthDay: String,
        nickname: String,
        imageUri: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = UserInfoUiState.Updating

            try {
                // 생년월일 포맷팅 (ISO 8601 형식)
                val birthDate =
                    if (birthYear.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank()) {
                        String.format(
                            "%04d-%02d-%02d",
                            birthYear.toIntOrNull() ?: 2000,
                            birthMonth.toIntOrNull() ?: 1,
                            birthDay.toIntOrNull() ?: 1
                        )
                    } else {
                        ""
                    }

                // 입력값 검증

                if (nickname.isBlank()) {
                    _uiState.value = UserInfoUiState.Error("닉네임을 입력해주세요")
                    return@launch
                }

                if (birthDate.isBlank()) {
                    _uiState.value = UserInfoUiState.Error("생년월일을 선택해주세요")
                    return@launch
                }

//                // 현재 사용자 정보 가져오기
//                val currentUser = when (val currentState = _uiState.value) {
//                    is UserInfoUiState.Success -> currentState.user
//                    else -> {
//                        _uiState.value = UserInfoUiState.Error("사용자 정보를 불러올 수 없습니다")
//                        return@launch
//                    }
//                }

                // 프로필 업데이트 API 호출
                when (val result = userRepository.updateUserProfile(
                    nickname = nickname,
                    birthDate = birthDate,
                    imageUri = imageUri,
                )) {
                    is Result.Success -> {
                        val updatedUser = result.data
                        Timber.d("사용자 프로필 업데이트 성공: ${updatedUser.nickname}")
                        // userInput도 업데이트
                        _userInput.value = UserInput(
                            nickname = updatedUser.nickname ?: "",
                            birthDate = updatedUser.birthDate ?: "",
                            sex = updatedUser.sex ?: Sex.MALE,
                            imageName = updatedUser.imageName,
                        )
                        _uiState.value = Success(updatedUser)
                    }

                    is Result.Error -> {
                        Timber.e(result.exception, "사용자 프로필 업데이트 실패")
                        _uiState.value = Error(result.message ?: "프로필 업데이트에 실패했습니다")
                    }

                    is Result.Loading -> {

                    }

                }
            } catch (e: Exception) {
                Timber.e(e, "프로필 업데이트 중 예외 발생")
                _uiState.value = UserInfoUiState.Error("프로필 업데이트에 실패했습니다")
            }
        }
    }

    /**
     * 사용자 입력 상태 업데이트
     */
    fun updateUserInput(input: UserInput) {
        _userInput.value = input
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


