package team.swyp.sdu.ui.record.components

import androidx.compose.runtime.Composable
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import java.time.LocalDate
import java.time.YearMonth

/**
 * 기록 탭 내용 컴포넌트
 */
@Composable
fun RecordTabContent(
    selectedTab: RecordTabType,
    monthStats: WalkAggregate,
    weekStats: WalkAggregate,
    monthSessions: List<WalkingSession>,
    weekSessions: List<WalkingSession>,
    monthMissionsCompleted: List<String>,
    currentDate: LocalDate,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToDailyRecord: (String) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
) {
    when (selectedTab) {
        RecordTabType.Month -> MonthSection(
            stats = monthStats,
            sessions = monthSessions,
            missionsCompleted = monthMissionsCompleted,
            onNavigateToDailyRecord = onNavigateToDailyRecord,
            onMonthChanged = onMonthChanged,
        )

        RecordTabType.Week -> WeekSection(
            stats = weekStats,
            currentDate = currentDate,
            onPrevWeek = onPrevWeek,
            onNextWeek = onNextWeek,
            sessions = weekSessions,
        )
    }
}



