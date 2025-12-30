package team.swyp.sdu.ui.login.terms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.repository.NotificationRepository
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * 약관 동의 UI 상태
 */
data class TermsAgreementUiState(
    val termsAgreed: Boolean = false,
    val privacyAgreed: Boolean = false,
    val locationAgreed: Boolean = false,
    val marketingConsent: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * 필수 약관이 모두 동의되었는지 확인
     * 금융/위치 서비스에서 사용하는 안전한 검증 방식
     */
    val canProceed: Boolean
        get() = termsAgreed && privacyAgreed && locationAgreed

    /**
     * 전체 동의 여부 확인
     */
    val isAllAgreed: Boolean
        get() = termsAgreed && privacyAgreed && locationAgreed && marketingConsent
}

/**
 * 약관 동의 ViewModel
 */
@HiltViewModel
class TermsAgreementViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TermsAgreementUiState())
    val uiState: StateFlow<TermsAgreementUiState> = _uiState.asStateFlow()

    /**
     * 서비스 이용약관 동의 상태 변경
     */
    fun updateTermsAgreed(agreed: Boolean) {
        _uiState.value = _uiState.value.copy(termsAgreed = agreed, errorMessage = null)
    }

    /**
     * 개인정보 처리방침 동의 상태 변경
     */
    fun updatePrivacyAgreed(agreed: Boolean) {
        _uiState.value = _uiState.value.copy(privacyAgreed = agreed, errorMessage = null)
    }

    /**
     * 위치 정보 이용약관 동의 상태 변경
     */
    fun updateLocationAgreed(agreed: Boolean) {
        _uiState.value = _uiState.value.copy(locationAgreed = agreed, errorMessage = null)
    }

    /**
     * 마케팅 정보 수신 동의 상태 변경
     */
    fun updateMarketingConsent(consent: Boolean) {
        _uiState.value = _uiState.value.copy(marketingConsent = consent, errorMessage = null)
    }

    /**
     * 전체 동의하기
     * 모든 약관을 동의/해제합니다.
     */
    fun toggleAllAgreements(agreed: Boolean) {
        _uiState.value = _uiState.value.copy(
            termsAgreed = agreed,
            privacyAgreed = agreed,
            locationAgreed = agreed,
            marketingConsent = agreed,
            errorMessage = null,
        )
    }


    /**
     * 약관 동의 제출
     * 필수 약관이 모두 체크되어야만 API 호출
     */
    fun submitTermsAgreement(
        onTermsAgreedUpdated: () -> Unit, // 약관 동의 상태 업데이트 콜백 추가
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val currentState = _uiState.value

        // 안전한 검증: 필수 약관이 모두 체크되었는지 확인
        if (!currentState.canProceed) {
            val missingTerms = mutableListOf<String>()
            if (!currentState.termsAgreed) missingTerms.add("서비스 이용약관")
            if (!currentState.privacyAgreed) missingTerms.add("개인정보 처리방침")
            if (!currentState.locationAgreed) missingTerms.add("위치 정보 이용약관")

            val errorMsg = "다음 약관에 동의해주세요: ${missingTerms.joinToString(", ")}"
            _uiState.value = currentState.copy(errorMessage = errorMsg)
            onError(errorMsg)
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

            when (val result = userRepository.agreeToTerms(
                termsAgreed = currentState.termsAgreed,
                privacyAgreed = currentState.privacyAgreed,
                locationAgreed = currentState.locationAgreed,
                marketingConsent = currentState.marketingConsent,
            )) {
                is Result.Success -> {
                    _uiState.value = currentState.copy(isLoading = false)
                    Timber.d("약관 동의 성공")

                    // 약관 동의 상태 업데이트 콜백 호출 (OnboardingViewModel에서 상태 저장)
                    onTermsAgreedUpdated()

                    // 약관 동의 상태가 업데이트된 후 콜백 호출
                    onSuccess()
                }
                is Result.Error -> {
                    val errorMsg = result.message ?: "약관 동의 중 오류가 발생했습니다"
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = errorMsg,
                    )
                    Timber.e(result.exception, "약관 동의 실패")
                    onError(errorMsg)
                }
                is Result.Loading -> {
                    // 이미 로딩 상태로 설정됨
                }
            }
        }
    }


}
