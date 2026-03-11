package swyp.team.walkit.analytics

/**
 * 화면 추적 및 이벤트 로깅 인터페이스.
 * 구현체: FirebaseAnalyticsTracker
 * 테스트: FakeAnalyticsTracker로 Mock 가능
 */
interface AnalyticsTracker {
    fun logScreenView(screen: AnalyticsScreen)
}
