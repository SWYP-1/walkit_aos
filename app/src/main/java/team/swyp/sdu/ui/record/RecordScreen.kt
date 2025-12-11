package team.swyp.sdu.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.ui.record.components.DaySection
import team.swyp.sdu.ui.record.components.HeaderRow
import team.swyp.sdu.ui.record.components.MonthSection
import team.swyp.sdu.ui.record.components.WeekSection

private enum class RecordTab { Month, Week, Day }

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
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
        dayStats = dayStats,
        weekStats = weekStats,
        monthStats = monthStats,
        daySessions = daySessions,
        currentDateLabel = currentDate.toString(),
        onPrevDay = { viewModel.prevDay() },
        onNextDay = { viewModel.nextDay() },
        allSessions = allSessions,
    )
}

@Composable
private fun RecordScreenContent(
    modifier: Modifier = Modifier,
    dummyMessage: String?,
    onDummyClick: () -> Unit,
    dayStats: WalkAggregate,
    weekStats: WalkAggregate,
    monthStats: WalkAggregate,
    daySessions: List<team.swyp.sdu.data.model.WalkingSession>,
    currentDateLabel: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    allSessions: List<team.swyp.sdu.data.model.WalkingSession>,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = RecordTab.entries

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderRow(onDummyClick = onDummyClick) }

        item {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color(0xFFE7E7E7),
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.large)
                    .fillMaxWidth()
                    .height(52.dp),
                divider = {},
                indicator = {},
            ) {
                tabs.forEachIndexed { index, item ->
                    val selected = tabIndex == index
                    Tab(
                        selected = selected,
                        onClick = { tabIndex = index },
                        modifier =
                            Modifier
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                        text = {
                            Text(
                                text =
                                    when (item) {
                                        RecordTab.Month -> "월간"
                                        RecordTab.Week -> "주간"
                                        RecordTab.Day -> "일간"
                                    },
                                color =
                                    if (selected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        selectedContentColor = Color.White,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            when (tabs[tabIndex]) {
                RecordTab.Month -> MonthSection(monthStats, allSessions)
                RecordTab.Week -> WeekSection(weekStats)
                RecordTab.Day -> DaySection(
                    stats = dayStats,
                    sessions = daySessions,
                    dateLabel = currentDateLabel,
                    onPrev = onPrevDay,
                    onNext = onNextDay,
                )
            }
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
