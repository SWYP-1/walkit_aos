package swyp.team.walkit.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import swyp.team.walkit.core.onError
import swyp.team.walkit.core.onSuccess
import swyp.team.walkit.data.local.datastore.OnboardingDataStore
import swyp.team.walkit.domain.model.OnboardingData
import swyp.team.walkit.domain.model.OnboardingProgress
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.domain.repository.GoalRepository
import timber.log.Timber
import java.time.LocalDate


data class NicknameState(
    val value: String = "",
    val isDuplicate: Boolean? = null,
    val validationError: String? = null
) {
    /**
     * 닉네임 유효성 검증
     * - 최소 1자 ~ 최대 20자
     * - 한글/영문(대소문자 구분) 가능
     * - 특수문자 불가능
     * - 띄어쓰기 불가능
     */
    val isValid: Boolean
        get() = validationError == null

    companion object {
        private const val MAX_NICKNAME_LENGTH = 20

        // 에러 메시지 상수
        private const val ERROR_TOO_LONG = "닉네임은 최대 20자까지 입력 가능합니다"
        private const val ERROR_HAS_SPACE = "닉네임에 띄어쓰기를 사용할 수 없습니다"

        fun validateNickname(nickname: String): String? {
            if (nickname.isEmpty()) return null

            return when {
                nickname.length > MAX_NICKNAME_LENGTH -> ERROR_TOO_LONG
                nickname.contains(" ") -> ERROR_HAS_SPACE
                !nickname.matches(Regex("^[가-힣a-zA-Z]*$")) -> ERROR_INVALID_CHARS
                else -> null
            }
        }
    }
}

/**
 * 온보딩 UI 상태
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val serviceTermsChecked: Boolean = false,
    val privacyPolicyChecked: Boolean = false,
    val marketingConsentChecked: Boolean = false,
    val nicknameState: NicknameState = NicknameState(),
    val goalCount: Int = 10,
    val stepTarget: Int = 0,
    val birthYear: Int = 0,
    val birthMonth: Int = 0,
    val birthDay: Int = 0,
    val isLoading: Boolean = false,
) {
    /**
     * 현재 단계에서 다음 버튼 활성화 여부
     */
    val canProceed: Boolean
        get() = when (currentStep) {
            0 -> nicknameState.isValid && nicknameState.isDuplicate == false && nicknameState.value.isNotBlank()
            1 -> {
                val yearValid = birthYear in 1901..LocalDate.now().year
                val monthValid = birthMonth in 1..12
                val dayValid = try {
                    LocalDate.of(birthYear, birthMonth, birthDay)
                    true
                } catch (t: Throwable) {
                    false
                }
                yearValid && monthValid && dayValid
            }

            2 -> {
                goalCount > 0 && stepTarget > 0
            }

            else -> false
        }

    /**
     * 생년월일 문자열 생성 (ISO 8601 형식)
     */
    val birthDateString: String
        get() = String.format("%04d-%02d-%02d", birthYear, birthMonth, birthDay)
}
private const val ERROR_INVALID_CHARS = "닉네임은 한글과 영문만 사용할 수 있습니다"

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val onboardingDataStore: OnboardingDataStore,
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val localCompleted = MutableStateFlow(false)

    val isCompleted: Flow<Boolean> =
        combine(onboardingDataStore.isCompleted, localCompleted) { fromDs, local ->
            fromDs || local
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val isTermsAgreed: Flow<Boolean> = onboardingDataStore.isTermsAgreed


    init {
        Timber.d("OnboardingViewModel init 시작")

        // 진행 상태를 확인하여 적절한 단계로 이동
        viewModelScope.launch {
            val progress = onboardingDataStore.getProgress()
            val isCompleted = onboardingDataStore.isCompleted.first()

            Timber.d("온보딩 상태 확인 - 진행 단계: ${progress.currentStep}, 닉네임: ${progress.nickname}, 닉네임등록: ${progress.nicknameRegistered}, completeKey: $isCompleted")

            // 약관 동의는 이미 완료된 상태로 온보딩에 진입
            // 완료 상태는 LoginScreen에서 확인하므로 여기서는 설정하지 않음
            // (화면 깜박임 방지를 위해)

            val targetStep = when (progress.currentStep) {
                0 -> {
                    // 닉네임 단계
                    Timber.d("닉네임 입력 단계(0)로 복귀")
                    0
                }

                1 -> {
                    // 생년월일 단계였지만, 닉네임이 등록되지 않았으면 닉네임 단계로 돌아감
                    if (!progress.nicknameRegistered || progress.nickname.isBlank()) {
                        Timber.d("닉네임이 등록되지 않음 → 닉네임 입력 단계(0)로 복귀")
                        0
                    } else {
                        // 닉네임까지 완료 (중복 체크 통과) → 생년월일 단계로 복귀
                        Timber.d("닉네임까지 완료됨 → 생년월일 단계(1)로 복귀")
                        1
                    }
                }

                2 -> {
                    // 목표 설정 단계였지만, 이전 단계들이 완료되지 않았으면 이전 단계로 돌아감
                    when {
                        !progress.nicknameRegistered || progress.nickname.isBlank() -> {
                            Timber.d("닉네임이 등록되지 않음 → 닉네임 입력 단계(0)로 복귀")
                            0
                        }

                        try {
                            val yearValid = progress.birthYear in 1901..LocalDate.now().year
                            val monthValid = progress.birthMonth in 1..12
                            val dayValid = try {
                                LocalDate.of(progress.birthYear, progress.birthMonth, progress.birthDay)
                                true
                            } catch (t: Throwable) {
                                false
                            }
                            !(yearValid && monthValid && dayValid)
                        } catch (t: Throwable) {
                            true
                        } -> {
                            Timber.d("생년월일이 유효하지 않음 → 생년월일 단계(1)로 복귀")
                            1
                        }

                        else -> {
                            // 모든 이전 단계 완료 → 목표 설정 단계로 복귀
                            Timber.d("이전 단계 모두 완료 → 목표 설정 단계(2)로 복귀")
                            2
                        }
                    }
                }

                else -> {
                    // 알 수 없는 단계 → 닉네임 단계부터 시작
                    Timber.d("알 수 없는 단계(${progress.currentStep}) → 닉네임 입력 단계(0)부터 시작")
                    0
                }
            }

            _uiState.value = OnboardingUiState(
                currentStep = targetStep,
                serviceTermsChecked = true,
                privacyPolicyChecked = true,
                marketingConsentChecked = progress.marketingConsent,
                nicknameState = NicknameState(
                    value = progress.nickname,
                    isDuplicate = if (progress.nicknameRegistered) false else null
                ),
                goalCount = progress.goalCount,
                stepTarget = progress.stepTarget,
                birthYear = progress.birthYear,
                birthMonth = progress.birthMonth,
                birthDay = progress.birthDay,
            )

            Timber.d("온보딩 상태 복원 완료 - targetStep: $targetStep")
        }
    }

    /**
     * 진행 상태 자동 저장 (단계 제외)
     * 입력값 변경 시 사용
     */
    private fun saveProgress() {
        val currentState = _uiState.value
        viewModelScope.launch {
            val existingProgress = onboardingDataStore.getProgress()
            val progress = OnboardingProgress(
                currentStep = existingProgress.currentStep, // 기존 단계 유지 (버그 수정)
                nickname = currentState.nicknameState.value,
                goalCount = currentState.goalCount,
                stepTarget = currentState.stepTarget,
                birthYear = currentState.birthYear,
                birthMonth = currentState.birthMonth,
                birthDay = currentState.birthDay,
                marketingConsent = currentState.marketingConsentChecked,
                nicknameRegistered = currentState.nicknameState.isDuplicate == false && currentState.nicknameState.value.isNotBlank(),
            )
            onboardingDataStore.saveProgress(progress)
        }
    }

    /**
     * 진행 상태 저장 (단계 포함)
     * API 성공 시 단계를 올릴 때만 사용
     */
    private fun saveProgressWithStep(step: Int) {
        val currentState = _uiState.value
        val progress = OnboardingProgress(
            currentStep = step,
            nickname = currentState.nicknameState.value,
            goalCount = currentState.goalCount,
            stepTarget = currentState.stepTarget,
            birthYear = currentState.birthYear,
            birthMonth = currentState.birthMonth,
            birthDay = currentState.birthDay,
            marketingConsent = currentState.marketingConsentChecked,
            nicknameRegistered = currentState.nicknameState.isDuplicate == false && currentState.nicknameState.value.isNotBlank(),
        )
        viewModelScope.launch {
            onboardingDataStore.saveProgress(progress)
        }
    }

    /**
     * 다음 단계로 이동 (API 성공 시에만 호출)
     */
    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 2) {
            val nextStep = current + 1
            _uiState.value = _uiState.value.copy(currentStep = nextStep)
            // 단계를 올릴 때만 단계를 포함하여 저장
            saveProgressWithStep(nextStep)
        }
    }

    /**
     * 이전 단계로 이동
     */
    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            val prevStep = current - 1
            _uiState.value = _uiState.value.copy(currentStep = prevStep)
            // 이전 단계로 이동할 때도 단계를 포함하여 저장
            saveProgressWithStep(prevStep)
        }
    }

    /**
     * 약관 동의 상태 업데이트
     */
    fun updateServiceTermsChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(serviceTermsChecked = checked)
        saveProgress()
        checkAndSaveTermsAgreement()
    }

    fun updatePrivacyPolicyChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(privacyPolicyChecked = checked)
        saveProgress()
        checkAndSaveTermsAgreement()
    }

    /**
     * 약관 동의 완료 상태 확인 및 저장
     */
    private fun checkAndSaveTermsAgreement() {
        val state = _uiState.value
        Timber.d("약관 동의 상태 확인: serviceTerms=${state.serviceTermsChecked}, privacyPolicy=${state.privacyPolicyChecked}")

        if (state.serviceTermsChecked && state.privacyPolicyChecked) {
            // 필수 약관들이 모두 동의된 경우 약관 동의 완료로 저장
            Timber.d("필수 약관 모두 동의됨, 약관 동의 완료 상태 저장")
            viewModelScope.launch {
                onboardingDataStore.setTermsAgreed(true)
                Timber.d("약관 동의 완료 상태 저장 - 성공")
            }
        } else {
            Timber.d("필수 약관이 아직 모두 동의되지 않음")
        }
    }


    /**
     * 닉네임 업데이트
     */
    fun updateNickname(nickname: String) {
        val validationError = NicknameState.validateNickname(nickname)
        _uiState.value = _uiState.value.copy(
            nicknameState = NicknameState(
                value = nickname,
                validationError = validationError
            )
        )
        saveProgress()
    }

    /**
     * 닉네임 중복 체크
     */
    fun checkNicknameDuplicate() {
        val state = _uiState.value
        val nickname = state.nicknameState.value

        // 유효성 검증 실패 시 중복 체크하지 않음
        if (!state.nicknameState.isValid || nickname.isBlank()) {
            Timber.w("닉네임 유효성 검증 실패 - 중복 체크 취소")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            userRepository.registerNickname(nickname)
                .onSuccess { isDuplicate ->
                    Timber.d("닉네임 중복 체크 결과: $nickname -> $isDuplicate")
                    _uiState.value = _uiState.value.copy(
                        nicknameState = state.nicknameState.copy(),
                        isLoading = false
                    )
                    saveProgress()
                }
                .onError { throwable, message ->
                    Timber.e(throwable, "닉네임 중복 체크 실패: $message")

                    when (throwable.message) {
                        "NICKNAME_VALIDATION_ERROR" -> {
                            // 닉네임 규칙 위반 - validationError 설정

                            _uiState.value = _uiState.value.copy(
                                nicknameState = state.nicknameState.copy(validationError = ERROR_INVALID_CHARS),
                                isLoading = false
                            )
                        }
                        else -> {
                            // 기타 에러 - 중복 체크 실패로 처리
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    }
                }
        }
    }

    /**
     * 목표 설정 업데이트
     * UI 레벨에서 범위 검증을 수행하므로 여기서는 직접 저장
     */
    fun updateGoalCount(goalCount: Int) {
        _uiState.value = _uiState.value.copy(goalCount = goalCount)
        saveProgress()
    }

    /**
     * 걸음 수 목표 업데이트
     * UI 레벨에서 범위 검증을 수행하므로 여기서는 직접 저장
     */
    fun updateStepTarget(stepTarget: Int) {
        _uiState.value = _uiState.value.copy(stepTarget = stepTarget)
        saveProgress()
    }

    /**
     * 출생년도 업데이트
     */
    fun updateBirthYear(birthYear: Int) {
        _uiState.value = _uiState.value.copy(birthYear = birthYear)
        saveProgress()
    }

    /**
     * 출생월 업데이트
     */
    fun updateBirthMonth(birthMonth: Int) {
        _uiState.value = _uiState.value.copy(birthMonth = birthMonth)
        saveProgress()
    }

    /**
     * 출생일 업데이트
     */
    fun updateBirthDay(birthDay: Int) {
        _uiState.value = _uiState.value.copy(birthDay = birthDay)
        saveProgress()
    }

    /**
     * 닉네임 등록
     */
    fun registerNickname() {
        Timber.d("registerNickname() 호출됨")
        val state = _uiState.value
        Timber.d("현재 닉네임 상태: value=${state.nicknameState.value}, isDuplicate=${state.nicknameState.isDuplicate}, validationError=${state.nicknameState.validationError}")


        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            Timber.d("registerNickname() - API 호출 시작: ${state.nicknameState.value}")
            userRepository.registerNickname(state.nicknameState.value)
                .onSuccess {
                    Timber.d("닉네임 등록 성공: ${state.nicknameState.value}")
                    // 중복 체크 통과 상태로 업데이트
                    _uiState.value = _uiState.value.copy(
                        nicknameState = state.nicknameState.copy(isDuplicate = false),
                        isLoading = false
                    )
                    // 진행 상태 저장 (nicknameRegistered = true로 저장됨)
                    saveProgress()
                    // 다음 단계로 진행
                    nextStep()
                }
                .onError { throwable, message ->
                    Timber.e(throwable, "닉네임 등록 실패: $message")

                    when (throwable.message) {
                        "NICKNAME_DUPLICATE_ERROR" -> {
                            // 중복 에러 - isDuplicate를 true로 설정
                            _uiState.value = _uiState.value.copy(
                                nicknameState = state.nicknameState.copy(isDuplicate = true),
                                isLoading = false
                            )
                        }
                        "NICKNAME_VALIDATION_ERROR" -> {
                            // 유효성 에러 - validationError 설정
                            _uiState.value = _uiState.value.copy(
                                nicknameState = state.nicknameState.copy(validationError = ERROR_INVALID_CHARS),
                                isLoading = false
                            )
                        }
                        else -> {
                            // 기타 에러 - 중복 에러로 처리
                            _uiState.value = _uiState.value.copy(
                                nicknameState = state.nicknameState.copy(isDuplicate = true),
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    /**
     * 생년월일 업데이트
     */
    fun updateBirthDate() {
        val state = _uiState.value
        val birthDate = state.birthDateString

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            Timber.d("생년월일 진입: $birthDate")

            userRepository.updateBirthDate(birthDate)
                .onSuccess {
                    Timber.d("생년월일 업데이트 성공: $birthDate")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // 진행 상태 저장
                    saveProgress()
                    // 다음 단계로 진행
                    nextStep()
                }
                .onError { throwable, message ->
                    Timber.e(throwable, "생년월일 업데이트 실패: $message")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // TODO: 에러 처리 (UI에 에러 표시)
                }
        }
    }

    /**
     * 목표 생성 API 호출 (POST /goals)
     * 온보딩 시 목표를 처음 생성할 때 사용
     */
    fun setGoal(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 범위 검증: API 호출 전에 값이 유효한지 확인
            val goalCount = _uiState.value.goalCount.coerceIn(1, 7)
            val stepTarget = _uiState.value.stepTarget.coerceIn(1000, 100000)

            // 검증된 값으로 UI 상태 업데이트 (범위를 벗어난 경우 자동 수정)
            if (_uiState.value.goalCount != goalCount || _uiState.value.stepTarget != stepTarget) {
                Timber.w("목표 값이 범위를 벗어남 - 자동 수정: goalCount=${_uiState.value.goalCount}->$goalCount, stepTarget=${_uiState.value.stepTarget}->$stepTarget")
                _uiState.value = _uiState.value.copy(
                    goalCount = goalCount,
                    stepTarget = stepTarget
                )
                saveProgress()
            }

            val goal = Goal(
                targetStepCount = stepTarget,
                targetWalkCount = goalCount
            )

            goalRepository.createGoal(goal)
                .onSuccess { createdGoal ->
                    Timber.d("목표 생성 성공: $createdGoal")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                .onError { throwable, message ->
                    Timber.e(throwable, "목표 생성 실패: $message")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // TODO: 에러 처리 (UI에 에러 표시)
                }
        }
    }

    /**
     * 온보딩 완료
     */
    fun submitOnboarding(onSuccess: () -> Unit = {}) {
        val state = _uiState.value

        viewModelScope.launch {
            try {
                // 온보딩 데이터 저장 (목표 설정 등)
                val onboardingData = OnboardingData(
                    nickname = state.nicknameState.value,
                    goalCount = state.goalCount,
                    stepTarget = state.stepTarget,
                    birthYear = state.birthYear,
                    marketingConsent = state.marketingConsentChecked
                )
                Timber.d("온보딩 데이터 저장: $onboardingData")

                // 완료 상태 저장 및 모든 온보딩 데이터 초기화
                localCompleted.value = true
                onboardingDataStore.completeOnboarding()

                Timber.d("온보딩 완료 성공 - onSuccess 콜백 호출")
                // 성공 시 콜백 호출 (화면 전환을 위해 필수)
                onSuccess()
            } catch (t: Throwable) {
                Timber.e(t, "온보딩 완료 실패")
                // TODO: 에러 처리
                throw t
            }
        }
    }
}


