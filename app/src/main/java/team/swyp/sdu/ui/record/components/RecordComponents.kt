package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.home.components.EmotionIcon
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 헤더 행 컴포넌트
 */
@Composable
fun HeaderRow(
    onDummyClick: () -> Unit,
    onStartOnboarding: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "기록",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Row {
            Button(
                onClick = onDummyClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                ),
            ) {
                Text("더미 데이터")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onStartOnboarding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                ),
            ) {
                Text("온보딩 시작")
            }
        }
    }
}

/**
 * 월간 섹션 컴포넌트
 */
@Composable
fun MonthSection(
    stats: WalkAggregate,
    sessions: List<WalkingSession>,
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val emotionsByDate = remember(sessions) {
        emptyMap<LocalDate, List<team.swyp.sdu.data.model.Emotion>>()
    }

    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val monthlyStats = remember(sessions, currentMonth) {
        calculateMonthlyStatsForRecord(sessions, currentMonth)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MonthNavigator(
            currentMonth = currentMonth,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
        )

        CalendarGridRecord(
            yearMonth = currentMonth,
            sessionsByDate = sessionsByDate,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        EmotionCard(
            title = "이번달 주요 감정",
            emotion = monthlyStats.primaryMood,
            desc = monthlyStats.description,
        )

        // 월간 통계 카드 (평균 걸음, 산책 시간)
        WalkingStatsCard(
            sessions = sessions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 주간 섹션 컴포넌트
 */
@Composable
fun WeekSection(
    stats: WalkAggregate,
    currentDate: LocalDate,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    sessions: List<WalkingSession> = emptyList(),
) {
    // 주간 날짜 범위 계산 (월요일 ~ 일요일)
    val startOfWeek = currentDate.with(DayOfWeek.MONDAY)
    val weekDates = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    // 세션을 날짜별로 그룹화
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    // 해당 주의 세션 필터링
    val weekSessions = remember(sessions, startOfWeek) {
        val endOfWeek = startOfWeek.plusDays(6)
        sessions.filter { session ->
            val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            !sessionDate.isBefore(startOfWeek) && !sessionDate.isAfter(endOfWeek)
        }
    }

    // 주요 감정 계산: postWalkEmotion 기준으로 가장 빈도가 높은 감정 찾기
    val dominantEmotionInfo = remember(weekSessions) {
        val emotionFrequency = weekSessions
            .mapNotNull { it.postWalkEmotion } // null이 아닌 감정만 필터링
            .groupingBy { it }
            .eachCount()

        if (emotionFrequency.isNotEmpty()) {
            // 가장 빈도가 높은 감정 찾기
            val mostFrequentEmotion = emotionFrequency.maxByOrNull { it.value }?.key
            val frequency = emotionFrequency[mostFrequentEmotion] ?: 0
            val emotionName = getEmotionKoreanName(mostFrequentEmotion)
            val description = "${emotionName}을(를) 이번 주에 ${frequency}회 경험했어요!"
            Pair(emotionName, description)
        } else {
            Pair("보통", "이번 주의 주요 감정입니다.")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 주간 네비게이터
        WeekNavigator(
            currentDate = currentDate,
            onPreviousWeek = onPrevWeek,
            onNextWeek = onNextWeek,
        )

        // 주간 캘린더
        WeekCalendarGrid(
            weekDates = weekDates,
            sessionsByDate = sessionsByDate,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        EmotionCard(
            title = "이번주 나의 주요 감정은?",
            emotion = dominantEmotionInfo.first,
            desc = dominantEmotionInfo.second,
        )

        // 주간 통계 카드 (평균 걸음, 산책 시간)
        WalkingStatsCard(
            sessions = weekSessions,
            modifier = Modifier.fillMaxWidth(),
        )

        GoalCheckRow()
    }
}

/**
 * 일간 섹션 컴포넌트
 */
@Composable
fun DaySection(
    stats: WalkAggregate,
    sessions: List<WalkingSession>,
    dateLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceAtMost((sessions.size - 1).coerceAtLeast(0)) }
    }
    val totalDistanceMeters by remember(sessions) {
        mutableStateOf(
            sessions.fold(0.0) { acc, session ->
                acc + computeRouteDistanceMeters(session.locations).coerceAtLeast(session.totalDistance.toDouble())
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 날짜 네비게이터
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "이전 날짜")
            }

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "다음 날짜")
            }
        }

        // 일간 통계 카드 (평균 걸음, 산책 시간)
        WalkingStatsCard(
            sessions = sessions,
            modifier = Modifier.fillMaxWidth(),
        )

        // 세션 목록 (좌우 스크롤)
        if (sessions.isNotEmpty()) {
//            LazyRow(
//                state = listState,
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.fillMaxWidth(),
//            ) {
//                items(sessions.size) { index ->
//                    val session = sessions[index]
//                    WalkingDiaryCard(session = session)
//                }
//            }
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(sessions.size) { index ->
                    WalkingDiaryCard(
                        session = sessions[index],
                        modifier = Modifier
                            .fillParentMaxWidth()
                    )
                }
            }

        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "이 날짜에 산책 기록이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }
        }
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "이전 달")
        }

        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 MM월")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "다음 달")
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "이전 주")
        }

        Text(
            text = weekLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        IconButton(onClick = onNextWeek) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "다음 주")
        }
    }
}

/**
 * 주간 라벨 포맷팅 함수
 * 예: "12월 첫째주", "12월 둘째주"
 * 
 * 해당 주의 시작일(월요일)이 속한 월을 기준으로,
 * 그 월의 첫 번째 날이 포함된 주를 첫째주로 계산합니다.
 */
private fun formatWeekLabel(date: LocalDate): String {
    val startOfWeek = date.with(DayOfWeek.MONDAY)
    val month = startOfWeek.monthValue
    val year = startOfWeek.year
    
    // 해당 월의 첫 번째 날
    val firstDayOfMonth = LocalDate.of(year, month, 1)
    
    // 첫 번째 날이 속한 주의 시작일(월요일) 찾기
    val firstDayWeekStart = when (val dayOfWeek = firstDayOfMonth.dayOfWeek.value) {
        1 -> firstDayOfMonth // 월요일이면 그대로
        else -> firstDayOfMonth.minusDays((dayOfWeek - 1).toLong()) // 이전 월요일
    }
    
    // 주차 계산 (첫 번째 날이 속한 주의 시작일로부터 몇 주째인지)
    val weekNumber = ((startOfWeek.toEpochDay() - firstDayWeekStart.toEpochDay()) / 7).toInt() + 1
    
    // 주차를 한글로 변환
    val weekLabel = when (weekNumber) {
        1 -> "첫째주"
        2 -> "둘째주"
        3 -> "셋째주"
        4 -> "넷째주"
        5 -> "다섯째주"
        6 -> "여섯째주"
        else -> "${weekNumber}째주"
    }
    
    return "${month}월 $weekLabel"
}

/**
 * 캘린더 그리드 컴포넌트
 */
@Composable
private fun CalendarGridRecord(
    yearMonth: YearMonth,
    sessionsByDate: Map<LocalDate, List<WalkingSession>>,
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

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
                                .aspectRatio(1f)
                                .padding(4.dp),
                        )
                    } else if (dayIndex < daysInMonth) {
                        val date = yearMonth.atDay(dayIndex + 1)
                        val hasWalkSession = sessionsByDate[date]?.isNotEmpty() == true

                        CalendarDayCellRecord(
                            day = dayIndex + 1,
                            hasWalkSession = hasWalkSession,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp),
                        )
                        dayIndex++
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 캘린더 데이 셀 컴포넌트
 */
@Composable
private fun CalendarDayCellRecord(
    day: Int,
    hasWalkSession: Boolean,
    modifier: Modifier = Modifier,
) {
    // 기본 구현: 날짜 숫자 표시
    val backgroundColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )

            // 산책 세션이 있으면 초록색 점 표시
            if (hasWalkSession) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)) // Green 색상
                )
            }
        }
    }
}

/**
 * 주간 캘린더 그리드 컴포넌트
 */
@Composable
private fun WeekCalendarGrid(
    weekDates: List<LocalDate>,
    sessionsByDate: Map<LocalDate, List<WalkingSession>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 요일 헤더
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
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // 주간 날짜 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            weekDates.forEach { date ->
                val hasWalkSession = sessionsByDate[date]?.isNotEmpty() == true
                WeekCalendarDayCell(
                    date = date,
                    hasWalkSession = hasWalkSession,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                )
            }
        }
    }
}

/**
 * 주간 캘린더 데이 셀 컴포넌트
 */
@Composable
private fun WeekCalendarDayCell(
    date: LocalDate,
    hasWalkSession: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )

            // 산책 세션이 있으면 초록색 점 표시
            if (hasWalkSession) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)) // Green 색상
                )
            }
        }
    }
}

/**
 * 감정 카드 컴포넌트
 */
@Composable
fun EmotionCard(
    title: String,
    emotion: String,
    desc: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF97FFB5),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = emotion,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * 통계 행 컴포넌트
 */
@Composable
fun StatsRow(items: List<StatItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * 세션 아이템 컴포넌트 (좌우 스크롤용)
 */
@Composable
private fun SessionItem(
    session: WalkingSession,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .width(280.dp) // 가로 스크롤을 위한 고정 너비
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "산책 세션",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "걸음 수: ${session.stepCount}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "거리: %.2f km".format(session.totalDistance / 1000),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * 섹션 카드 컴포넌트
 */
@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            content()
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
    val primaryMood: String,
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
    val monthStart = month.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthEnd = month.atEndOfMonth().atTime(23, 59, 59)
        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

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
    val emotionFrequency = monthSessions
        .mapNotNull { it.postWalkEmotion } // null이 아닌 감정만 필터링
        .groupingBy { it }
        .eachCount()
    
    val primaryMood: String
    val description: String
    
    if (emotionFrequency.isNotEmpty()) {
        // 가장 빈도가 높은 감정 찾기
        val mostFrequentEmotion = emotionFrequency.maxByOrNull { it.value }?.key
        val frequency = emotionFrequency[mostFrequentEmotion] ?: 0
        
        primaryMood = getEmotionKoreanName(mostFrequentEmotion)
        description = "${primaryMood}을(를) 이번 달에 ${frequency}회 경험했어요!"
    } else {
        // 감정 데이터가 없으면 기본값
        primaryMood = "보통"
        description = "이번 달의 주요 감정입니다."
    }

    return MonthlyStatsRecord(
        primaryMood = primaryMood,
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
private fun getEmotionKoreanName(emotionType: team.swyp.sdu.data.model.EmotionType?): String =
    when (emotionType) {
        team.swyp.sdu.data.model.EmotionType.HAPPY -> "기쁨"
        team.swyp.sdu.data.model.EmotionType.JOYFUL -> "즐거움"
        team.swyp.sdu.data.model.EmotionType.CONTENT -> "행복함"
        team.swyp.sdu.data.model.EmotionType.DEPRESSED -> "우울함"
        team.swyp.sdu.data.model.EmotionType.TIRED -> "지침"
        team.swyp.sdu.data.model.EmotionType.ANXIOUS -> "짜증남"
        null -> "보통"
    }

/**
 * 감정 타입에 따른 색상과 이모지 반환
 */
fun getMoodColorAndEmojiRecord(emotionType: team.swyp.sdu.data.model.EmotionType?): Pair<Color, String> =
    when (emotionType) {
        team.swyp.sdu.data.model.EmotionType.HAPPY -> Color(0xFFFFF59D) to "😊"
        team.swyp.sdu.data.model.EmotionType.JOYFUL -> Color(0xFFFFD54F) to "🤩"
        team.swyp.sdu.data.model.EmotionType.CONTENT -> Color(0xFF81C784) to "😄"
        team.swyp.sdu.data.model.EmotionType.DEPRESSED -> Color(0xFF90A4AE) to "😔"
        team.swyp.sdu.data.model.EmotionType.TIRED -> Color(0xFFB0BEC5) to "😴"
        team.swyp.sdu.data.model.EmotionType.ANXIOUS -> Color(0xFF80DEEA) to "😰"
        null -> Color.White to "-"
    }

/**
 * 산책 시간 포맷팅 함수
 * 0시간보다 작으면 분으로 표시, 그 외에는 시간과 분으로 표시
 */
private fun formatWalkingTime(totalMinutes: Long): String {
    if (totalMinutes < 60) {
        // 1시간 미만이면 분으로만 표시
        return "${totalMinutes}분"
    } else {
        // 1시간 이상이면 시간과 분으로 표시
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes > 0) {
            "${hours}시간 ${minutes}분"
        } else {
            "${hours}시간"
        }
    }
}

/**
 * 경로 거리 계산 함수 (간단 버전)
 */
private fun computeRouteDistanceMeters(locations: List<team.swyp.sdu.data.model.LocationPoint>): Double {
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
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
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
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 상단: 감정 아이콘들 + 더보기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 감정 아이콘들
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 산책 전 감정
                    session.preWalkEmotion?.let { emotion ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EmotionIcon(emotionType = emotion)
                                }
                            }
                        }
                    }

                    // 산책 후 감정
                    session.postWalkEmotion?.let { emotion ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EmotionIcon(emotionType = emotion)
                                }
                            }
                        }
                    }
                }

                // 더보기 버튼
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    
                    // 더보기 메뉴
                    DiaryMoreMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onEditClick = {
                            showMenu = false
                            onEditClick()
                        },
                        onDeleteClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                    )
                }
            }

            // 구분선
            HorizontalDivider(
                color = Color(0xFFF3F3F5),
                thickness = 1.dp,
            )

            // 산책 일기 제목
            Text(
                text = "산책 일기",
                style = MaterialTheme.walkItTypography.bodyS,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
            )

            // 일기 내용 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9F9FA))
                    .padding(10.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    text = session.note ?: "감정 일기 내용",
                    style = MaterialTheme.walkItTypography.captionM,
                    color = Grey7,
                    maxLines = Int.MAX_VALUE,
                )
            }
        }
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
        modifier = modifier
            .width(160.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        ) {
            // 수정하기 메뉴 아이템
            DiaryMenuItem(
                text = "수정하기",
                icon = Icons.Default.Edit,
                iconColor = Color(0xFF52CE4B), // 초록색
                textColor = Color(0xFF52CE4B), // 초록색
                backgroundColor = Color(0xFFF3FFF8), // 연한 초록색 배경
                onClick = onEditClick,
            )
            
            // 삭제하기 메뉴 아이템
            DiaryMenuItem(
                text = "삭제하기",
                icon = Icons.Default.Delete,
                iconColor = Color(0xFF191919), // 검은색
                textColor = Color(0xFF191919), // 검은색
                backgroundColor = Color.White, // 흰색 배경
                onClick = onDeleteClick,
            )
        }
    }
}

/**
 * 산책 통계 카드 컴포넌트 (평균 걸음, 산책 시간)
 * 월간, 주간, 일간 모두에서 사용 가능
 */
@Composable
fun WalkingStatsCard(
    sessions: List<WalkingSession>,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    text = "평균 걸음",
                    style = MaterialTheme.walkItTypography.captionM,
                    color = Grey10,
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%,d".format(averageSteps),
                        style = MaterialTheme.walkItTypography.headingS,
                        color = Grey10,
                    )
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
                    text = "산책 시간",
                    style = MaterialTheme.walkItTypography.captionM,
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

/**
 * 일기 메뉴 아이템 컴포넌트
 */
@Composable
private fun DiaryMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    textColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}

@Composable
@Preview
fun WalkingDiaryCardPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        WalkingDiaryCard(session = WalkingSession(
            startTime = 1,
        ))
    }
}