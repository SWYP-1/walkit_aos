package swyp.team.walkit.analytics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * NavGraph route 변경 시 screen_view 로깅을 담당.
 * previousRoute를 ViewModel에서 보관하여 recomposition에 안전하게 유지.
 */
@HiltViewModel
class NavigationAnalyticsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    var previousRoute: String? = null
        private set

    fun onRouteChanged(routePattern: String) {
        if (previousRoute == routePattern) return
        previousRoute = routePattern

        routeToAnalyticsScreen(routePattern)?.let { screen ->
            analyticsTracker.logScreenView(screen)
        }
    }
}
