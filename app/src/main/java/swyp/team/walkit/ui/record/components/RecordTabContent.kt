package swyp.team.walkit.ui.record.components

import androidx.compose.runtime.Composable
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.model.MissionProgress
import swyp.team.walkit.presentation.viewmodel.CalendarViewModel.WalkAggregate
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
    goal: Goal,
    missionProgress: MissionProgress = MissionProgress.None,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToDailyRecord: (String) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onClaimMissionReward: (Long) -> Unit = {},
    onUpdateNote: (id: String, note: String) -> Unit = { _, _ -> },
    onDeleteNote: (id: String) -> Unit = {},
) {
    when (selectedTab) {
        RecordTabType.Month -> MonthSectionSafe(
            stats = monthStats,
            sessions = monthSessions,
            missionsCompleted = monthMissionsCompleted,
            onNavigateToDailyRecord = onNavigateToDailyRecord,
            onMonthChanged = onMonthChanged,
            onUpdateNote = onUpdateNote,
            onDeleteNote = onDeleteNote,
        )

        RecordTabType.Week -> WeekSectionSafe(
            stats = weekStats,
            currentDate = currentDate,
            goal = goal,
            onPrevWeek = onPrevWeek,
            onNextWeek = onNextWeek,
            sessions = weekSessions,
            missionProgress = missionProgress,
            onClaimMissionReward = onClaimMissionReward,
        )
    }
}



