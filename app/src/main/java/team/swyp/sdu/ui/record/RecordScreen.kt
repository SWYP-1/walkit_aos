package team.swyp.sdu.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.ui.record.components.DaySection
import team.swyp.sdu.ui.record.components.HeaderRow
import team.swyp.sdu.ui.record.components.MonthSection
import team.swyp.sdu.ui.record.components.RecordHeader
import team.swyp.sdu.ui.record.components.RecordTabRow
import team.swyp.sdu.ui.record.components.RecordTabType
import team.swyp.sdu.ui.record.components.WeekSection
import java.time.LocalDate

@Composable
fun RecordRoute(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateToFriend: () -> Unit = {},
    onNavigateToAlarm: () -> Unit = {},
    onStartOnboarding: () -> Unit = {},
) {
    val dummyMessage by viewModel.dummyMessage.collectAsStateWithLifecycle()
    val dayStats by viewModel.dayStats.collectAsStateWithLifecycle()
    val weekStats by viewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by viewModel.monthStats.collectAsStateWithLifecycle()
    val daySessions by viewModel.daySessions.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    RecordScreenContent(
        modifier = modifier,
        dummyMessage = dummyMessage,
        onDummyClick = { viewModel.generateDummyData() },
        onStartOnboarding = onStartOnboarding,
        dayStats = dayStats,
        weekStats = weekStats,
        monthStats = monthStats,
        daySessions = daySessions,
        currentDateLabel = currentDate.toString(),
        currentDate = currentDate,
        onPrevDay = { viewModel.prevDay() },
        onNextDay = { viewModel.nextDay() },
        onPrevWeek = { viewModel.prevWeek() },
        onNextWeek = { viewModel.nextWeek() },
        onNavigateToAlarm = onNavigateToAlarm,
        onNavigateToFriend = onNavigateToFriend,
        allSessions = allSessions,
    )
}

@Composable
private fun RecordScreenContent(
    modifier: Modifier = Modifier,
    dummyMessage: String?,
    onDummyClick: () -> Unit,
    onStartOnboarding: () -> Unit,
    dayStats: WalkAggregate,
    weekStats: WalkAggregate,
    monthStats: WalkAggregate,
    daySessions: List<team.swyp.sdu.data.model.WalkingSession>,
    currentDateLabel: String,
    currentDate: LocalDate,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToFriend: () -> Unit,
    onNavigateToAlarm: () -> Unit,
    allSessions: List<team.swyp.sdu.data.model.WalkingSession>,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = RecordTabType.entries

    Column {
        RecordHeader(onClickAlarm = { onNavigateToAlarm }, onClickSearch = { onNavigateToFriend() })
        LazyColumn(
            modifier =
                modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            item { HeaderRow(onDummyClick = onDummyClick, onStartOnboarding = onStartOnboarding) }

            item {
                RecordTabRow(
                    selectedTabIndex = tabIndex,
                    onTabSelected = { tabIndex = it },
                )
            }

            item {
                RecordTabContent(
                    selectedTab = tabs[tabIndex],
                    monthStats = monthStats,
                    weekStats = weekStats,
                    dayStats = dayStats,
                    daySessions = daySessions,
                    allSessions = allSessions,
                    currentDate = currentDate,
                    currentDateLabel = currentDateLabel,
                    onPrevWeek = onPrevWeek,
                    onNextWeek = onNextWeek,
                    onPrevDay = onPrevDay,
                    onNextDay = onNextDay,
                )
            }

            item {
                dummyMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }


}

/**
 * 기록 탭 내용 컴포넌트
 */
@Composable
private fun RecordTabContent(
    selectedTab: RecordTabType,
    monthStats: WalkAggregate,
    weekStats: WalkAggregate,
    dayStats: WalkAggregate,
    daySessions: List<team.swyp.sdu.data.model.WalkingSession>,
    allSessions: List<team.swyp.sdu.data.model.WalkingSession>,
    currentDate: LocalDate,
    currentDateLabel: String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    when (selectedTab) {
        RecordTabType.Month -> MonthSection(monthStats, allSessions)
        RecordTabType.Week -> WeekSection(
            stats = weekStats,
            currentDate = currentDate,
            onPrevWeek = onPrevWeek,
            onNextWeek = onNextWeek,
            sessions = allSessions,
        )

        RecordTabType.Day -> DaySection(
            stats = dayStats,
            sessions = daySessions,
            dateLabel = currentDateLabel,
            onPrev = onPrevDay,
            onNext = onNextDay,
        )
    }
}
