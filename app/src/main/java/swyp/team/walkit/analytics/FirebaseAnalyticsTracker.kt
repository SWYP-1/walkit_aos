package swyp.team.walkit.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics 기반 화면 추적 구현체.
 * CustomTest 프로덕션 차단은 routeToAnalyticsScreen 매핑 단계에서 처리.
 */
class FirebaseAnalyticsTracker(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {

    override fun logScreenView(screen: AnalyticsScreen) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
