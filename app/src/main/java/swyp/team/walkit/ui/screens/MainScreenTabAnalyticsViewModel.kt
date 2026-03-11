package swyp.team.walkit.ui.screens

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import swyp.team.walkit.analytics.AnalyticsScreen
import swyp.team.walkit.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * MainScreen 탭별 screen_view 로깅 담당.
 * previousTabIndex로 동일 탭 재클릭 시 중복 로깅 방지.
 */
@HiltViewModel
class MainScreenTabAnalyticsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    var previousTabIndex: Int? = null
        private set

    fun onTabChanged(tabIndex: Int) {
        if (previousTabIndex == tabIndex) return
        previousTabIndex = tabIndex

        val screen = when (tabIndex) {
            0 -> AnalyticsScreen.MainHome
            1 -> AnalyticsScreen.MainRecord
            2 -> AnalyticsScreen.MainCharacter
            3 -> AnalyticsScreen.MainMyPage
            else -> return
        }

        analyticsTracker.logScreenView(screen)
    }
}
