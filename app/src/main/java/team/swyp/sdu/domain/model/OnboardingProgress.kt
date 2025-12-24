package team.swyp.sdu.domain.model

/**
 * 온보딩 진행 상태 모델
 *
 * 사용자가 온보딩 중간에 앱을 종료하고 다시 시작하는 경우를 위한 저장용 모델
 */
data class OnboardingProgress(
    val currentStep: Int = 0,
    val nickname: String = "",
    val selectedImageUri: String? = null,
    val sex: Sex = Sex.MALE, // nullable 제거, 기본값 설정
    val goalCount: Int = 10,
    val stepTarget: Int = 0,
    val unit: String = "달",
    val birthYear: Int = 1990,
    val birthMonth: Int = 1,
    val birthDay: Int = 1,
    val marketingConsent: Boolean = false,
    val nicknameRegistered: Boolean = false, // 닉네임이 중복 체크를 통과하고 등록되었는지 여부
) {
    companion object {
        val EMPTY = OnboardingProgress()
    }
}
