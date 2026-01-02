package team.swyp.sdu.core

import team.swyp.sdu.BuildConfig

/**
 * 민감한 정보 관리 예시
 * 실제 코드에서는 이 클래스의 패턴을 따라 API 키를 사용하세요
 */
object ConfigExample {

    // ✅ 안전한 방법: BuildConfig에서 가져오기
    val kakaoAppKey: String
        get() = BuildConfig.KAKAO_APP_KEY

    val naverClientId: String
        get() = BuildConfig.NAVER_CLIENT_ID

    val naverClientSecret: String
        get() = BuildConfig.NAVER_CLIENT_SECRET

    // ❌ 위험한 방법: 코드에 직접 하드코딩 (절대 사용하지 마세요)
//    const val KAKAO_APP_KEY_DANGEROUS = "your_actual_app_key_here" // 위험!

    // 디버그 모드에서만 사용할 수 있는 키 검증
    fun validateKeys(): Boolean {
        return kakaoAppKey.isNotBlank() &&
               naverClientId.isNotBlank() &&
               naverClientSecret.isNotBlank()
    }
}
