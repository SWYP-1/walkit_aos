package swyp.team.walkit.ui.record.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.ui.walking.utils.stringToEmotionType
import swyp.team.walkit.ui.walking.utils.stringToEmotionTypeOrNull
import swyp.team.walkit.presentation.viewmodel.CalendarViewModel.WalkAggregate
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.ui.components.SectionCard
import swyp.team.walkit.ui.home.components.DominantEmotionCard
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.model.MissionProgress
import swyp.team.walkit.ui.theme.Green1
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Modifier extension for custom shadow effect
 */


fun Modifier.customShadow(): Modifier =
    this.dropShadow(
        shape = RoundedCornerShape(12.dp),
        shadow = Shadow(
            radius = 10.dp,
            color = Color.Black.copy(alpha = 0.06f),
            offset = DpOffset(0.dp, 0.dp)
        )
    )

/**
 * Modifier extension for card border stroke
 */
fun Modifier.cardBorder(): Modifier = this.border(
    width = 1.dp,
    color = SemanticColor.textBorderSecondaryInverse,
    shape = RoundedCornerShape(12.dp)
)

/**
 * 안전하게 epochMilli를 LocalDate로 변환
 * 예외 발생 시 오늘 날짜 반환
 */
fun safeEpochMilliToLocalDate(epochMilli: Long): LocalDate {
    return try {
        Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (e: Exception) {
        LocalDate.now()
    }
}


/**
 * 월간 섹션 컴포넌트 (릴리즈 안전 버전)
 */
@Composable
fun MonthSectionSafe(
    stats: WalkAggregate,
    sessions: List<WalkingSession>,
    missionsCompleted: List<String>,
    onNavigateToDailyRecord: (String) -> Unit = {},
    onMonthChanged: (YearMonth) -> Unit = {},
    onUpdateNote: (id: String, note: String) -> Unit = { _, _ -> },
    onDeleteNote: (id: String) -> Unit = {},
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            safeEpochMilliToLocalDate(session.startTime)
        }
    }

    // 선택된 날짜의 세션만 필터링 (최신순)
    val selectedDaySessions = remember(sessions, selectedDate) {
        val date = selectedDate ?: return@remember emptyList()
        sessions
            .filter { safeEpochMilliToLocalDate(it.startTime) == date }
            .sortedByDescending { it.startTime }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Column(
            Modifier
                .fillMaxWidth()
                .customShadow()
                .background(SemanticColor.backgroundWhitePrimary, shape = RoundedCornerShape(12.dp))
        ) {
            MonthNavigator(
                currentMonth = currentMonth,
                onPreviousMonth = {
                    currentMonth = currentMonth.minusMonths(1)
                    onMonthChanged(currentMonth)
                    selectedDate = if (YearMonth.from(LocalDate.now()) == currentMonth)
                        LocalDate.now() else null
                },
                onNextMonth = {
                    currentMonth = currentMonth.plusMonths(1)
                    onMonthChanged(currentMonth)
                    selectedDate = if (YearMonth.from(LocalDate.now()) == currentMonth)
                        LocalDate.now() else null
                },
            )

            CalendarGridRecord(
                yearMonth = currentMonth,
                sessionsByDate = sessionsByDate,
                missionsCompleted = missionsCompleted,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // 선택된 날짜의 산책 일지
        selectedDaySessions.forEach { session ->
            key(session.id) {
                var isEditing by remember { mutableStateOf(false) }
                var editedNote by remember(session.id) { mutableStateOf(session.note ?: "") }

                // debounce 자동 저장: 타이핑 멈춘 후 800ms 뒤 저장
                LaunchedEffect(editedNote) {
                    kotlinx.coroutines.delay(800)
                    if (editedNote != (session.note ?: "")) {
                        onUpdateNote(session.id, editedNote)
                    }
                }

                // safety net: 날짜 변경 등으로 컴포저블이 사라질 때 미저장 내용 저장
                DisposableEffect(session.id) {
                    onDispose {
                        if (editedNote != (session.note ?: "")) {
                            onUpdateNote(session.id, editedNote)
                        }
                    }
                }

                WalkingDiaryCard(
                    session = session,
                    note = editedNote,
                    isEditMode = isEditing,
                    setEditing = { isEditing = it },
                    onNoteChange = { editedNote = it },
                    onDeleteClick = { onDeleteNote(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}


/**
 * 주간 섹션 컴포넌트
 */
/**
 * 주간 섹션 컴포넌트 (릴리즈 안전 버전)
 */
@Composable
fun WeekSectionSafe(
    stats: WalkAggregate,
    currentDate: LocalDate,
    goal: Goal,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    sessions: List<WalkingSession> = emptyList(),
    missionProgress: MissionProgress = MissionProgress.None,
    onClaimMissionReward: (Long) -> Unit = {},
) {
    // 현재 월과 연도
    val currentMonth = currentDate.month
    val currentYear = currentDate.year

    // 해당 주의 전체 날짜 범위 (일요일 시작)
    val startOfWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val weekDates = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }


    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            safeEpochMilliToLocalDate(session.startTime)
        }
    }

    val weekSessions = sessions

    val dominantEmotionInfo = remember(weekSessions) {
        try {
            val emotionFrequency = weekSessions.map { session ->
                stringToEmotionType(session.postWalkEmotion)
            }.groupingBy { it }.eachCount()

            if (emotionFrequency.isNotEmpty()) {
                val mostFrequentEmotion = emotionFrequency.maxByOrNull { it.value }?.key
                val frequency = emotionFrequency[mostFrequentEmotion] ?: 0
                Pair(mostFrequentEmotion, frequency)
            } else {
                Pair(null as EmotionType?, 0)
            }
        } catch (e: Exception) {
            Pair(null, 0)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Column(
            Modifier
                .fillMaxWidth()
                .customShadow()
                .cardBorder()
                .background(SemanticColor.backgroundWhitePrimary, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            WeekNavigator(
                currentDate = currentDate,
                onPreviousWeek = onPrevWeek,
                onNextWeek = onNextWeek,
            )
            Spacer(Modifier.height(13.dp))

            WeekCalendarGrid(
                weekDates = weekDates,
                sessionsByDate = sessionsByDate,
            )
        }

        WeeklyGoalBarChartCard(
            weekDates = weekDates,
            sessionsByDate = sessionsByDate,
            goal = goal,
            modifier = Modifier.fillMaxWidth(),
        )

        MissionProgressCard(
            progress = missionProgress,
            modifier = Modifier.fillMaxWidth(),
            onClaimReward = onClaimMissionReward,
        )

        WalkingStatsCard(
            sessions = weekSessions,
            modifier = Modifier
                .fillMaxWidth()
                .cardBorder(),
        )

        DominantEmotionCard(
            emotionType = dominantEmotionInfo.first,
            emotionCount = dominantEmotionInfo.second,
            periodText = "이번주",
            modifier = Modifier.cardBorder()
        )

        Spacer(Modifier.height(22.dp))
    }
}

/**
 * 월 네비게이터 컴포넌트
 */
@Composable
private fun MonthNavigator(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar_left),
                "이전 달",
                tint = SemanticColor.iconGrey
            )
        }

        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 MM월")),
            style = MaterialTheme.walkItTypography.bodyL.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.textBorderPrimary
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar_right),
                "다음 달", tint = SemanticColor.iconGrey
            )
        }
    }
}

/**
 * 주간 네비게이터 컴포넌트
 */
@Composable
private fun WeekNavigator(
    currentDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val weekLabel = remember(currentDate) {
        formatWeekLabel(currentDate)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 35.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar_left),
                contentDescription = "이전 주",
                tint = Color(0xFFD9D9D9)
            )
        }

        Text(
            text = weekLabel,
            style = MaterialTheme.walkItTypography.bodyL.copy(
                fontWeight = FontWeight.Medium
            ),
        )

        IconButton(onClick = onNextWeek) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar_right),
                contentDescription = "다음 주",
                tint = Color(0xFFD9D9D9),
            )
        }
    }
}

/**
 * 주간 라벨 포맷팅 함수
 * 예: "12월 첫째주", "12월 둘째주"
 *
 * 해당 주의 시작일(일요일)이 속한 월을 기준으로,
 * 그 월의 첫 번째 일요일을 첫째주 기준으로 계산합니다.
 */
fun formatWeekLabel(date: LocalDate): String {
    // 현재 날짜가 속한 주의 일요일 (일요일 시작 기준)
    val currentWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    val targetMonth = currentWeekStart.month
    val targetYear = currentWeekStart.year

    // 해당 월의 모든 일요일 찾기
    val firstDayOfMonth = LocalDate.of(targetYear, targetMonth, 1)
    val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

    // 해당 월의 첫 번째 일요일 찾기
    val firstSunday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    // 해당 월의 모든 일요일 수집
    val sundaysInMonth = mutableListOf<LocalDate>()
    var sunday = firstSunday
    while (!sunday.isAfter(lastDayOfMonth)) {
        sundaysInMonth.add(sunday)
        sunday = sunday.plusWeeks(1)
    }

    // 현재 주의 일요일이 해당 월의 몇 번째 일요일인지 확인
    val weekNumber = sundaysInMonth.indexOf(currentWeekStart) + 1

    val weekLabel = when (weekNumber) {
        1 -> "첫째주"
        2 -> "둘째주"
        3 -> "셋째주"
        4 -> "넷째주"
        5 -> "다섯째주"
        6 -> "여섯째주"
        else -> "${weekNumber}째주"
    }

    return "${targetMonth.value}월 $weekLabel"
}

/**
 * 캘린더 그리드 컴포넌트
 */
@Composable
private fun CalendarGridRecord(
    yearMonth: YearMonth,
    sessionsByDate: Map<LocalDate, List<WalkingSession>>,
    missionsCompleted: List<String>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    val missionsCompletedSet = remember(missionsCompleted) {
        missionsCompleted.toSet()
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

            }
        }
        Spacer(Modifier.height(10.dp))

        var dayIndex = 0
        repeat(6) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(7) { dayOfWeek ->
                    if (week == 0 && dayOfWeek < firstDayOfWeek) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.19.dp, vertical = 4.dp)
                                .aspectRatio(1f)
                        )
                    } else if (dayIndex < daysInMonth) {
                        val date = yearMonth.atDay(dayIndex + 1)
                        val sessions = sessionsByDate[date]
                        val hasWalkSession = sessions?.isNotEmpty() == true
                        val dateString =
                            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val hasMissionCompleted = missionsCompletedSet.contains(dateString)
                        val dominantEmotion = run {
                            val emotionList = sessions
                                ?.mapNotNull { stringToEmotionTypeOrNull(it.postWalkEmotion) }
                                ?: emptyList()
                            emotionList
                                .groupingBy { it }
                                .eachCount()
                                .maxWithOrNull(
                                    compareBy(
                                        { it.value },
                                        { emotionList.indexOfLast { e -> e == it.key } }
                                    )
                                )?.key
                        }

                        CalendarDayCellRecord(
                            date = date,
                            day = dayIndex + 1,
                            hasWalkSession = hasWalkSession,
                            hasMissionCompleted = hasMissionCompleted,
                            dominantEmotion = dominantEmotion,
                            isSelected = selectedDate != null && date == selectedDate,
                            onDateSelected = { onDateSelected(date) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.19.dp, vertical = 4.dp)
                                .aspectRatio(1f),
                        )
                        dayIndex++
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.19.dp, vertical = 4.dp)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 감정 타입을 캘린더 원형 배경 색상으로 변환
 */
private fun EmotionType.toCalendarCircleColor(): Color = when (this) {
    EmotionType.IRRITATED -> SemanticColor.stateRedSecondary
    EmotionType.DEPRESSED -> SemanticColor.stateBlueSecondary
    EmotionType.JOYFUL -> SemanticColor.stateYellowSecondary
    EmotionType.DELIGHTED -> SemanticColor.stateGreenSecondary
    EmotionType.TIRED -> SemanticColor.statePurpleSecondary
    EmotionType.HAPPY -> SemanticColor.statePinkSecondary
}

/**
 * 캘린더 데이 셀 컴포넌트
 *
 * 산책 기록이 있는 날짜는 감정 타입에 맞는 42dp 원형 배경 위에 날짜를 표시한다.
 */
@Composable
private fun CalendarDayCellRecord(
    date: LocalDate,
    day: Int,
    hasWalkSession: Boolean,
    hasMissionCompleted: Boolean,
    dominantEmotion: EmotionType?,
    isSelected: Boolean,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val isToday = date == today

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onDateSelected() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 오늘 배경 (선택된 경우에만 표시)
        if (isToday && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(SemanticColor.backgroundGreenSecondary)
                    .border(2.dp, SemanticColor.stateGreenPrimary, CircleShape)
            )
        }
        // 감정 색상 원형 배경 (오늘이 선택된 경우엔 녹색 배경이 우선)
        if (hasWalkSession && !(isToday && isSelected)) {
            val circleColor =
                dominantEmotion?.toCalendarCircleColor() ?: SemanticColor.stateGreenSecondary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(circleColor)
            )
        }
        // 선택된 날짜 링 표시 (오늘은 자체 스타일 유지)
        if (isSelected && !isToday) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .border(2.dp, SemanticColor.stateGreenPrimary, CircleShape)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF171717),
            )
            // 오늘 표시 점
//            if (isToday && !hasWalkSession) {
//                Spacer(Modifier.height(2.dp))
//                Box(
//                    modifier = Modifier
//                        .size(4.dp)
//                        .clip(CircleShape)
//                        .background(SemanticColor.stateBluePrimary)
//                )
//            }
            // 미션 완료 점
//            if (hasMissionCompleted) {
//                Spacer(Modifier.height(2.dp))
//                Box(
//                    modifier = Modifier
//                        .size(4.dp)
//                        .clip(CircleShape)
//                        .background(SemanticColor.stateAquaBlueSecondary)
//                )
//            }
        }
    }
}


/**
 * 주간 캘린더 그리드 컴포넌트
 *
 * 상단 요일 레이블(월~일) + 하단 날짜 원형 셀 구조.
 * 산책 기록이 있는 날짜는 감정 색 원 안에 숫자가 표시된다.
 */
@Composable
private fun WeekCalendarGrid(
    weekDates: List<LocalDate>,
    sessionsByDate: Map<LocalDate, List<WalkingSession>>,
    modifier: Modifier = Modifier,
) {
    // weekDates는 일~토 순서 (Sunday-based)
    val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(modifier = modifier) {
        // 요일 레이블 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            dayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 날짜 원형 셀 행
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.83.dp),
        ) {
            weekDates.forEach { date ->
                val sessions = sessionsByDate[date]
                val hasWalkSession = sessions?.isNotEmpty() == true
                val dominantEmotion = run {
                    val emotionList = sessions
                        ?.mapNotNull { stringToEmotionTypeOrNull(it.postWalkEmotion) }
                        ?: emptyList()
                    emotionList
                        .groupingBy { it }
                        .eachCount()
                        .maxWithOrNull(
                            compareBy(
                                { it.value },
                                { emotionList.indexOfLast { e -> e == it.key } }
                            )
                        )?.key
                }

                WeekCalendarDayCell(
                    day = date.dayOfMonth,
                    hasWalkSession = hasWalkSession,
                    dominantEmotion = dominantEmotion,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

/**
 * 주간 캘린더 데이 셀 컴포넌트
 *
 * 산책 기록이 있으면 감정 타입에 맞는 원형 배경 안에 날짜 숫자를 표시한다.
 * 산책 기록이 없으면 숫자만 표시한다.
 */
@Composable
private fun WeekCalendarDayCell(
    day: Int,
    hasWalkSession: Boolean,
    dominantEmotion: EmotionType?,
    modifier: Modifier = Modifier,
) {
    val circleColor = dominantEmotion?.toCalendarCircleColor() ?: SemanticColor.stateGreenSecondary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (hasWalkSession) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(circleColor)
            )
        }
        Text(
            text = day.toString(),
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF171717),
            textAlign = TextAlign.Center,
        )
    }
}


/**
 * 주간 목표달성률 막대그래프 카드
 *
 * 각 날짜별로 가장 걸음 수가 많은 단일 세션을 기준으로 목표 달성 여부를 판단한다.
 * 목표 달성(최고 세션 >= targetStepCount)이면 녹색, 미달이면 회색 막대를 표시한다.
 */
@Composable
internal fun WeeklyGoalBarChartCard(
    weekDates: List<LocalDate>,
    sessionsByDate: Map<LocalDate, List<WalkingSession>>,
    goal: Goal,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    // 날짜별 최고 단일 세션 걸음 수
    val bestStepsByDate: List<Int> = weekDates.map { date ->
        sessionsByDate[date]?.maxOfOrNull { it.stepCount } ?: 0
    }

    // 주간 달성 횟수: 단일 세션 기준으로 목표 이상인 세션의 총 개수
    val weeklyAchievedCount = sessionsByDate.values.sumOf { sessions ->
        sessions.count { it.stepCount >= goal.targetStepCount }
    }

    // Y축 최대 스케일: 목표 걸음 수가 항상 80dp 기준
    val maxScale = goal.targetStepCount.coerceAtLeast(1)
    val maxBarHeight = 80.dp

    Column(
        modifier = modifier
            .customShadow()
            .cardBorder()
            .background(SemanticColor.backgroundWhitePrimary, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "목표 달성률",
                style = MaterialTheme.walkItTypography.bodyM.copy(fontWeight = FontWeight.SemiBold),
                color = SemanticColor.textBorderPrimary,
            )
//            Text(
//                text = "목표 ${goal.targetWalkCount}회 중 ${weeklyAchievedCount}회 달성",
//                style = MaterialTheme.walkItTypography.captionM,
//                color = if (weeklyAchievedCount >= goal.targetWalkCount)
//                    SemanticColor.stateGreenPrimary else SemanticColor.textBorderSecondary,
//            )
        }

        Spacer(Modifier.height(8.dp))

        // 차트 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            weekDates.forEachIndexed { index, date ->
                val bestStep = bestStepsByDate[index]
                val hasWalk = bestStep > 0
                val barRatio = if (hasWalk) (bestStep.toFloat() / maxScale).coerceIn(0f, 1f) else 0f
                val isToday = date == today

                Column(
                    modifier = Modifier.width(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    // 막대 영역
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxBarHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {

                        // 막대
                        if (hasWalk) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(barRatio)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (isToday) SemanticColor.textBorderGreenPrimary
                                        else SemanticColor.stateGreenSecondary
                                    )
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 요일 레이블
                    Text(
                        text = dayLabels[index],
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isToday) SemanticColor.textBorderSecondary
                        else SemanticColor.textBorderSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * 목표 체크 행 컴포넌트
 */
@Composable
fun GoalCheckRow() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "주간 목표 달성률",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "70% 달성했어요!",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "완료",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/**
 * 통계 아이템 데이터 클래스
 */
data class StatItem(
    val title: String,
    val value: String,
)

/**
 * 월간 통계 데이터 클래스
 */
private data class MonthlyStatsRecord(
    val primaryMood: EmotionType?,
    val emotionCount: Int,
    val description: String,
    val totalSteps: Int,
    val averageSteps: Int, // 평균 걸음 수
    val walkingTimeMinutes: Long, // 산책 시간 (분)
    val sessionsCount: Int,
)

/**
 * 월간 통계 계산 함수
 */
private fun calculateMonthlyStatsForRecord(
    sessions: List<WalkingSession>,
    month: YearMonth,
): MonthlyStatsRecord {
    val monthStart =
        month.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthEnd =
        month.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant()
            .toEpochMilli()

    val monthSessions = sessions.filter { session ->
        session.startTime in monthStart..monthEnd
    }

    val totalSteps = monthSessions.sumOf { it.stepCount }
    val sessionsCount = monthSessions.size
    val averageSteps = if (sessionsCount > 0) {
        totalSteps / sessionsCount
    } else {
        0
    }

    // 산책 시간 계산 (밀리초 -> 분)
    val totalWalkingTimeMillis = monthSessions.sumOf { session ->
        session.duration
    }
    val walkingTimeMinutes = totalWalkingTimeMillis / (1000 * 60) // 밀리초를 분으로 변환

    // 주요 감정 계산: postWalkEmotion 기준으로 가장 빈도가 높은 감정 찾기
    val emotionFrequency = monthSessions.map { session ->
        stringToEmotionType(session.postWalkEmotion)
    }.groupingBy { it }.eachCount()

    val primaryMood: EmotionType?
    val description: String

    var emotionCount = 0

    if (emotionFrequency.isNotEmpty()) {
        // 가장 빈도가 높은 감정 찾기
        val mostFrequentEmotion = emotionFrequency.maxByOrNull { it.value }?.key
        emotionCount = emotionFrequency[mostFrequentEmotion] ?: 0

        primaryMood = mostFrequentEmotion
        val emotionName = getEmotionKoreanName(mostFrequentEmotion)
        description = "${emotionName}을(를) 이번 달에 ${emotionCount}회 경험했어요!"
    } else {
        // 감정 데이터가 없으면 기본값
        primaryMood = null
        emotionCount = 0
        description = "이번 달의 주요 감정입니다."
    }

    return MonthlyStatsRecord(
        primaryMood = primaryMood,
        emotionCount = emotionCount,
        description = description,
        totalSteps = totalSteps,
        averageSteps = averageSteps,
        walkingTimeMinutes = walkingTimeMinutes,
        sessionsCount = sessionsCount,
    )
}

/**
 * 감정 타입을 한글 이름으로 변환
 */
private fun getEmotionKoreanName(emotionType: swyp.team.walkit.data.model.EmotionType?): String =
    when (emotionType) {
        swyp.team.walkit.data.model.EmotionType.JOYFUL -> "기쁨"
        swyp.team.walkit.data.model.EmotionType.DELIGHTED -> "즐거움"
        swyp.team.walkit.data.model.EmotionType.HAPPY -> "행복함"
        swyp.team.walkit.data.model.EmotionType.DEPRESSED -> "우울함"
        swyp.team.walkit.data.model.EmotionType.TIRED -> "지침"
        swyp.team.walkit.data.model.EmotionType.IRRITATED -> "짜증남"
        null -> "보통"
    }

/**
 * 산책 시간 포맷팅 함수
 * 0시간보다 작으면 분으로 표시, 그 외에는 시간과 분으로 표시
 */
// FormatUtils로 통합됨 - formatWalkingTime은 FormatUtils.formatWalkingTime으로 대체

/**
 * 경로 거리 계산 함수 (간단 버전)
 */
private fun computeRouteDistanceMeters(locations: List<swyp.team.walkit.data.model.LocationPoint>): Double {
    if (locations.size < 2) return 0.0

    var totalDistance = 0.0
    for (i in 0 until locations.size - 1) {
        val loc1 = locations[i]
        val loc2 = locations[i + 1]

        val lat1 = loc1.latitude
        val lon1 = loc1.longitude
        val lat2 = loc2.latitude
        val lon2 = loc2.longitude

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(
            Math.toRadians(lat2)
        ) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        totalDistance += 6371000 * c // 지구 반지름 * c
    }

    return totalDistance
}

/**
 * 산책 일기 카드 컴포넌트
 */
@Composable
fun WalkingDiaryCard(
    session: WalkingSession,
    note: String,
    isEditMode: Boolean,
    setEditing: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // TextFieldValue를 사용하여 커서 위치 제어
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = note, selection = TextRange(note.length)))
    }

    // note가 변경될 때 TextFieldValue 업데이트 (편집 취소 등)
    LaunchedEffect(note) {
        if (!isEditMode) {
            textFieldValue = TextFieldValue(text = note, selection = TextRange(note.length))
        }
    }

    // 포커스 요청 시 텍스트 끝으로 커서 이동
    LaunchedEffect(isEditMode, focusRequester) {
        if (isEditMode && focusRequester != null) {
            focusRequester.requestFocus()
            // 약간의 딜레이 후 커서 위치 설정
            kotlinx.coroutines.delay(100)
            textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
        }
    }
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .customShadow()
            .background(
                color = SemanticColor.backgroundWhitePrimary,
                shape = RoundedCornerShape(12.dp)
            )
            .cardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    text = "감정 기록",
                    style = MaterialTheme.walkItTypography.bodyS,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                )

                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_more),
                        contentDescription = "더보기",
                        tint = SemanticColor.iconBlack
                    )
                }
            }

            // 상단: 감정 아이콘 + 더보기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 산책 전 감정 (String을 EmotionType으로 변환)
                    EmotionCircleIcon(stringToEmotionType(session.preWalkEmotion))
                    // 산책 후 감정 (String을 EmotionType으로 변환)
                    EmotionCircleIcon(stringToEmotionType(session.postWalkEmotion))
                }

                Box {

                    DiaryMoreMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onEditClick = {
                            showMenu = false
                            setEditing(true) // ✅ 함수 호출로 바꿔야 함
                            // 상위에서 isEditMode true로 관리
                        },
                        onDeleteClick = {
                            showMenu = false
                            onDeleteClick(session.id)
                        })
                }
            }
            // 일기 내용
            val innerPadding = 10.dp
            if (note.isNotEmpty() || isEditMode) {
                HorizontalDivider(color = Color(0xFFF3F3F5), thickness = 1.dp)
                SectionCard {
                    if (isEditMode) {
                        TextField(
                            value = textFieldValue, onValueChange = { newValue ->
                                textFieldValue = newValue
                                onNoteChange(newValue.text)
                            }, textStyle = MaterialTheme.walkItTypography.captionM.copy(
                                color = SemanticColor.textBorderSecondary
                            ), colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                            ), modifier = Modifier
                                .fillMaxWidth()
                                .height(138.dp)
                                .then(
                                    if (focusRequester != null) Modifier.focusRequester(
                                        focusRequester
                                    )
                                    else Modifier
                                )
                        )
                    } else {
                        Text(
                            text = note.ifEmpty { "감정 일기 내용" },
                            style = MaterialTheme.walkItTypography.bodyS,
                            color = SemanticColor.textBorderSecondary,
                            maxLines = Int.MAX_VALUE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(138.dp)
                                .padding(10.dp)
                        )
                    }
                }
            }


        }
    }
}

fun getCircleEmotionIcon(emotion: EmotionType): Int {
    return when (emotion) {
        EmotionType.JOYFUL -> R.drawable.ic_circle_joyful
        EmotionType.DELIGHTED -> R.drawable.ic_circle_delighted
        EmotionType.HAPPY -> R.drawable.ic_circle_happy
        EmotionType.DEPRESSED -> R.drawable.ic_circle_depressed
        EmotionType.TIRED -> R.drawable.ic_circle_tired
        EmotionType.IRRITATED -> R.drawable.ic_circle_anxious
    }
}

@Composable
fun EmotionCircleIcon(emotion: EmotionType, size: Dp = 52.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
    ) {
        Image(
            painter = painterResource(getCircleEmotionIcon(emotion = emotion)),
            contentDescription = "emotion circle "
        )
    }
}

/**
 * 산책 일기 더보기 메뉴 컴포넌트
 */
@Composable
private fun DiaryMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = SemanticColor.backgroundWhitePrimary,
        modifier = modifier.background(SemanticColor.backgroundWhitePrimary),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
        ) {
            DropMenuItem(
                text = "수정하기",
                iconResId = R.drawable.ic_action_edit,
                iconColor = SemanticColor.textBorderPrimary,
                textColor = SemanticColor.textBorderPrimary,
                backgroundColor = SemanticColor.backgroundWhitePrimary,
                onClick = onEditClick
            )
            DropMenuItem(
                text = "삭제하기",
                iconResId = R.drawable.ic_action_delete,
                iconColor = SemanticColor.textBorderPrimary,
                textColor = SemanticColor.textBorderPrimary,
                backgroundColor = SemanticColor.backgroundWhitePrimary,
                onClick = onDeleteClick
            )
        }
    }
}


/**
 * 산책 통계 카드 컴포넌트 (평균 걸음, 산책 시간)
 * 월간, 주간, 일간 모두에서 사용 가능
 *
 * @param sessions 통계를 계산할 세션 목록
 * @param modifier Modifier
 * @param stepsLabel 평균 걸음 수 라벨 (기본값: "평균 걸음")
 * @param durationLabel 총 산책 시간 라벨 (기본값: "누적 산책 시간")
 */
@Composable
fun WalkingStatsCard(
    sessions: List<WalkingSession>,
    modifier: Modifier = Modifier,
    stepsLabel: String = "평균 걸음",
    durationLabel: String = "누적 산책 시간",
) {
    // 평균 걸음 계산
    val averageSteps = remember(sessions) {
        if (sessions.isNotEmpty()) {
            sessions.sumOf { it.stepCount } / sessions.size
        } else {
            0
        }
    }

    // 총 산책 시간 계산 (밀리초)
    val totalDurationMillis = remember(sessions) {
        sessions.sumOf { it.duration }
    }

    // 시간과 분으로 변환
    val totalHours = (totalDurationMillis / (1000 * 60 * 60)).toInt()
    val totalMinutes = ((totalDurationMillis / (1000 * 60)) % 60).toInt()

    Card(
        modifier = modifier
            .customShadow()
            .cardBorder(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 평균 걸음 섹션
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stepsLabel,
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = Grey10,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%,d".format(averageSteps),
                        style = MaterialTheme.walkItTypography.headingS,
                        color = Grey10,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "걸음",
                        style = MaterialTheme.walkItTypography.bodyM,
                        color = Grey10,
                    )
                }
            }

            // 세로 구분선
            VerticalDivider(
                color = Color(0xFFD7D9E0),
                thickness = 1.dp,
                modifier = Modifier.height(40.dp),
            )

            // 산책 시간 섹션
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = durationLabel,
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = Grey10,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = totalHours.toString(),
                        style = MaterialTheme.walkItTypography.headingS,
                        color = Grey10,
                    )
                    Text(
                        text = "시간",
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = Grey10,
                    )
                    Text(
                        text = totalMinutes.toString(),
                        style = MaterialTheme.walkItTypography.headingS,
                        color = Grey10,
                    )
                    Text(
                        text = "분",
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = Grey10,
                    )
                }
            }
        }
    }
}

@Composable
fun DropMenuItem(
    text: String,
    iconResId: Int? = null,
    iconColor: Color = SemanticColor.iconBlack,
    textColor: Color = SemanticColor.iconBlack,
    backgroundColor: Color = SemanticColor.backgroundWhitePrimary,
    rippleColor: Color = Green1,
    pressedTextColor: Color = SemanticColor.buttonPrimaryDefault,
    pressedIconColor: Color = SemanticColor.buttonPrimaryDefault,
    pressedBackgroundColor: Color = Green1,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentTextColor = if (isPressed) pressedTextColor else textColor
    val currentIconColor = if (isPressed) pressedIconColor else iconColor
    val currentBackgroundColor = if (isPressed) pressedBackgroundColor else backgroundColor

    Box(
        modifier = modifier
            .height(32.dp)
            .background(currentBackgroundColor, shape = RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = rippleColor),
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = currentIconColor
                )
            }
            Text(
                text = text, style = MaterialTheme.walkItTypography.bodyS, color = currentTextColor
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun WalkingDiaryCardPreview() {
    WalkItTheme {
        // 더미 세션 데이터
        val dummySession = WalkingSession(
            id = "session_01",
            startTime = System.currentTimeMillis() - 3600_000, // 1시간 전
            stepCount = 3500,
            preWalkEmotion = "JOYFUL",
            postWalkEmotion = "HAPPY",
            locations = emptyList(), // 좌표 생략
            note = "오늘은 날씨가 좋아서 산책이 즐거웠어요.",
            endTime = 12314556L,
            createdDate = "24112556",
        )

        // 상태 관리용 remember
        var isEditMode by remember { mutableStateOf(false) }
        var noteText by remember { mutableStateOf(dummySession.note) }

        WalkingDiaryCard(
            session = dummySession,
            note = noteText.toString(),
            isEditMode = isEditMode,
            setEditing = { isEditMode = it },
            onNoteChange = { noteText = it },
            onDeleteClick = { id -> println("삭제 클릭: $id") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
@Preview(showBackground = true)
fun DiaryMoreMenuPreview() {
    WalkItTheme {
        // 프리뷰용 상태
        var showMenu by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd
        ) {
            if (showMenu) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 2.83.dp, vertical = 4.dp)
                ) {
                    DropMenuItem(
                        text = "수정하기",
                        iconResId = R.drawable.ic_action_edit,
                        iconColor = Color(0xFF191919),
                        textColor = Color(0xFF191919),
                        backgroundColor = Color.White,
                        onClick = {})

                    DropMenuItem(
                        text = "삭제하기가 더 길어지면",
                        iconResId = R.drawable.ic_action_delete,
                        iconColor = Color(0xFF191919),
                        textColor = Color(0xFF191919),
                        backgroundColor = Color.White,
                        onClick = {})
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "차트 — 목표 미달 (걸음 수 혼합)")
@Composable
private fun WeeklyGoalBarChartCardPreview() {
    WalkItTheme {
        val today = LocalDate.now()
        val weekDates = (0..6).map { today.with(DayOfWeek.MONDAY).plusDays(it.toLong()) }
        fun fakeSession(id: String, date: LocalDate, steps: Int) = WalkingSession(
            id = id,
            startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endTime = 0L,
            stepCount = steps,
            preWalkEmotion = "",
            postWalkEmotion = "",
            createdDate = date.toString(),
        )

        val sessionsByDate = mapOf(
            weekDates[0] to listOf(fakeSession("1", weekDates[0], 8_000)),
            weekDates[1] to listOf(fakeSession("2", weekDates[1], 12_000)),
            weekDates[2] to listOf(fakeSession("3", weekDates[2], 10_000)),
        )
        WeeklyGoalBarChartCard(
            weekDates = weekDates,
            sessionsByDate = sessionsByDate,
            goal = Goal(targetStepCount = 10_000, targetWalkCount = 3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "주간 캘린더 — 감정 원 다양")
@Composable
private fun WeekCalendarGridPreview() {
    WalkItTheme {
        val today = LocalDate.now()
        val weekDates = (0..6).map { today.with(DayOfWeek.MONDAY).plusDays(it.toLong()) }
        val emotions = listOf(
            EmotionType.JOYFUL,
            EmotionType.DELIGHTED,
            null,
            EmotionType.TIRED,
            EmotionType.HAPPY,
            EmotionType.IRRITATED,
            EmotionType.DEPRESSED,
        )

        fun fakeSession(id: String, date: LocalDate, emotion: EmotionType) = WalkingSession(
            id = id,
            startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endTime = 0L,
            stepCount = 8_000,
            preWalkEmotion = "",
            postWalkEmotion = emotion.name,
            createdDate = date.toString(),
        )

        val sessionsByDate = weekDates.mapIndexedNotNull { i, date ->
            val emotion = emotions[i] ?: return@mapIndexedNotNull null
            date to listOf(fakeSession("$i", date, emotion))
        }.toMap()

        WeekCalendarGrid(
            weekDates = weekDates,
            sessionsByDate = sessionsByDate,
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticColor.backgroundWhitePrimary)
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "월간 캘린더 — 감정 원 다양")
@Composable
private fun CalendarGridRecordPreview() {
    WalkItTheme {
        val yearMonth = YearMonth.now()
        val emotions = listOf(
            EmotionType.JOYFUL,
            EmotionType.DELIGHTED,
            EmotionType.TIRED,
            EmotionType.HAPPY,
            EmotionType.IRRITATED,
            EmotionType.DEPRESSED,
            null,
        )

        fun fakeSession(id: String, date: LocalDate, emotion: EmotionType) = WalkingSession(
            id = id,
            startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endTime = 0L,
            stepCount = 8_000,
            preWalkEmotion = "",
            postWalkEmotion = emotion.name,
            createdDate = date.toString(),
        )

        val sessionsByDate = (1..yearMonth.lengthOfMonth())
            .mapNotNull { day ->
                val date = yearMonth.atDay(day)
                val emotion = emotions[day % emotions.size] ?: return@mapNotNull null
                date to listOf(fakeSession("$day", date, emotion))
            }.toMap()

        CalendarGridRecord(
            yearMonth = yearMonth,
            sessionsByDate = sessionsByDate,
            missionsCompleted = emptyList(),
            selectedDate = LocalDate.now(),
            onDateSelected = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticColor.backgroundWhitePrimary)
                .padding(horizontal = 4.dp),
        )
    }
}
