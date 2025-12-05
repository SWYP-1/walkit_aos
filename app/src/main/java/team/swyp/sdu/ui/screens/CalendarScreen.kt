package team.swyp.sdu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.WalkingSessionListViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 캘린더 화면
 * 산책 기록을 날짜별로 캘린더에 표시합니다.
 */
@Composable
fun CalendarScreen(
    onNavigateToRouteDetail: (List<team.swyp.sdu.data.model.LocationPoint>) -> Unit,
    viewModel: WalkingSessionListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessions = when (val state = uiState) {
        is team.swyp.sdu.presentation.viewmodel.WalkingSessionListUiState.Success -> state.sessions
        else -> emptyList()
    }

    // 날짜별 세션 맵 생성
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
    )

    // 선택된 날짜 상태
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(
                    day = day,
                    hasSession = sessionsByDate.containsKey(day.date),
                    isSelected = selectedDate == day.date,
                    onClick = {
                        selectedDate = if (selectedDate == day.date) null else day.date
                    },
                )
            },
            monthHeader = { month ->
                MonthHeader(month = month)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 선택된 날짜의 세션 목록 표시
        selectedDate?.let { date ->
            val daySessions = sessionsByDate[date] ?: emptyList()

            if (daySessions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "${date.monthValue}월 ${date.dayOfMonth}일 기록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    daySessions.forEach { session ->
                        SessionItem(
                            session = session,
                            onClick = {
                                onNavigateToRouteDetail(session.locations)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 캘린더 날짜 셀
 */
@Composable
private fun Day(
    day: CalendarDay,
    hasSession: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    hasSession -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                },
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                day.position != DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                isSelected -> MaterialTheme.colorScheme.onPrimary
                hasSession -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 월 헤더
 */
@Composable
private fun MonthHeader(month: CalendarMonth) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = "${month.yearMonth.year}년 ${month.yearMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * 세션 아이템
 */
@Composable
private fun SessionItem(
    session: WalkingSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Column {
            Text(
                text = session.getFormattedDuration(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${session.getFormattedDistance()} · ${session.stepCount}걸음",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
