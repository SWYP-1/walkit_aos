package team.swyp.sdu.ui.record.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.components.SectionCard
import team.swyp.sdu.ui.home.components.DominantEmotionCard
import team.swyp.sdu.ui.home.components.EmotionIcon
import team.swyp.sdu.ui.theme.Green1
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Red1
import team.swyp.sdu.ui.theme.Red5
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.SemanticColor.backgroundWhitePrimary
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.FormatUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Modifier extension for custom shadow effect
 */
fun Modifier.customShadow(): Modifier = this.graphicsLayer {
    shadowElevation = 10f
    ambientShadowColor = Color(0x0F000000)
    spotShadowColor = Color(0x0F000000)
    shape = RoundedCornerShape(12.dp)
    clip = false
}

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
    missionsCompleted: List<String>,
    onNavigateToDailyRecord: (String) -> Unit, // 날짜 형식: "yyyy-MM-dd"
    onMonthChanged: (YearMonth) -> Unit = {}, // 월 변경 시 ViewModel에 알림
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val emotionsByDate = remember(sessions) {
        emptyMap<LocalDate, List<team.swyp.sdu.data.model.Emotion>>()
    }

    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }
    }

    val monthlyStats = remember(sessions, currentMonth) {
        calculateMonthlyStatsForRecord(sessions, currentMonth)
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
                },
                onNextMonth = {
                    currentMonth = currentMonth.plusMonths(1)
                    onMonthChanged(currentMonth)
                },
            )

            CalendarGridRecord(
                yearMonth = currentMonth,
                sessionsByDate = sessionsByDate,
                missionsCompleted = missionsCompleted,
                onNavigateToDailyRecord = onNavigateToDailyRecord,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // 월간 통계 카드 (평균 걸음, 산책 시간)
        WalkingStatsCard(
            sessions = sessions,
            modifier = Modifier.fillMaxWidth(),
        )

        DominantEmotionCard(
            emotionType = monthlyStats.primaryMood,
            emotionCnt = monthlyStats.emotionCount.toString(),
            periodText = "이번달",
        )
        Spacer(Modifier.height(16.dp))
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
    sessions: List<WalkingSession> = emptyList(), // 이미 해당 주의 세션만 필터링된 데이터
) {
    // 주간 날짜 범위 계산 (월요일 ~ 일요일)
    val startOfWeek = currentDate.with(DayOfWeek.MONDAY)
    val weekDates = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    // 세션을 날짜별로 그룹화 (이미 해당 주의 세션만 포함)
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }
    }

    // sessions는 이미 해당 주의 세션만 포함하므로 추가 필터링 불필요
    val weekSessions = sessions

    // 주요 감정 계산: postWalkEmotion 기준으로 가장 빈도가 높은 감정 찾기
    val dominantEmotionInfo = remember(weekSessions) {
        val emotionFrequency = weekSessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

        if (emotionFrequency.isNotEmpty()) {
            // 가장 빈도가 높은 감정 찾기
            val mostFrequentEmotion = emotionFrequency.maxByOrNull { it.value }?.key
            val frequency = emotionFrequency[mostFrequentEmotion] ?: 0
            Pair(mostFrequentEmotion, frequency.toString())
        } else {
            Pair(null as EmotionType?, "0")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Column(
            Modifier
                .fillMaxWidth()
                .customShadow()
                .background(SemanticColor.backgroundWhitePrimary, shape = RoundedCornerShape(12.dp))
        ) {
            // 주간 네비게이터
            WeekNavigator(
                currentDate = currentDate,
                onPreviousWeek = onPrevWeek,
                onNextWeek = onNextWeek,
            )
            Spacer(Modifier.height(13.dp))

            // 주간 캘린더
            WeekCalendarGrid(
                weekDates = weekDates,
                sessionsByDate = sessionsByDate,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }


        // 주간 통계 카드 (평균 걸음, 산책 시간)
        WalkingStatsCard(
            sessions = weekSessions,
            modifier = Modifier.fillMaxWidth(),
        )

        DominantEmotionCard(
            emotionType = dominantEmotionInfo.first,
            emotionCnt = dominantEmotionInfo.second,
            periodText = "이번주",
        )

//        GoalCheckRow()
        Spacer(Modifier.height(22.dp))
    }
}

///**
// * 일간 섹션 컴포넌트
// */
//@Composable
//fun DaySection(
//    stats: WalkAggregate,
//    sessions: List<WalkingSession>,
//    dateLabel: String,
//    onPrev: () -> Unit,
//    onNext: () -> Unit,
//) {
//    val listState = rememberLazyListState()
//    val scope = rememberCoroutineScope()
//    val currentIndex by remember {
//        derivedStateOf {
//            listState.firstVisibleItemIndex.coerceAtMost(
//                (sessions.size - 1).coerceAtLeast(
//                    0
//                )
//            )
//        }
//    }
//    val totalDistanceMeters by remember(sessions) {
//        mutableStateOf(
//            sessions.fold(0.0) { acc, session ->
//                acc + computeRouteDistanceMeters(session.locations).coerceAtLeast(session.totalDistance.toDouble())
//            },
//        )
//    }
//
//    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
//        // 날짜 네비게이터
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            IconButton(onClick = onPrev) {
//                Icon(Icons.AutoMirrored.Filled.ArrowBack, "이전 날짜")
//            }
//
//            Text(
//                text = dateLabel,
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//            )
//
//            IconButton(onClick = onNext) {
//                Icon(Icons.AutoMirrored.Filled.ArrowForward, "다음 날짜")
//            }
//        }
//
//        // 일간 통계 카드 (평균 걸음, 산책 시간)
//        WalkingStatsCard(
//            sessions = sessions,
//            modifier = Modifier.fillMaxWidth(),
//        )
//
//        // 세션 목록 (좌우 스크롤)
//        if (sessions.isNotEmpty()) {
////            LazyRow(
////                state = listState,
////                horizontalArrangement = Arrangement.spacedBy(8.dp),
////                modifier = Modifier.fillMaxWidth(),
////            ) {
////                items(sessions.size) { index ->
////                    val session = sessions[index]
////                    WalkingDiaryCard(session = session)
////                }
////            }
//            LazyRow(
//                state = listState,
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.fillMaxWidth(),
//            ) {
//                items(sessions.size) { index ->
//                    WalkingDiaryCard(
//                        session = sessions[index],
//                        onEditClick = {},
//                        onDeleteClick = {},
//                        modifier = Modifier
//                            .fillParentMaxWidth()
//                    )
//                }
//            }
//
//        } else {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp),
//                contentAlignment = Alignment.Center,
//            ) {
//                Text(
//                    text = "이 날짜에 산책 기록이 없습니다.",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = Color.Gray,
//                )
//            }
//        }
//    }
//}

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
            Icon(painter = painterResource(R.drawable.ic_calendar_left), "이전 달")
        }

        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 MM월")),
            style = MaterialTheme.walkItTypography.bodyL.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.textBorderPrimary
        )

        IconButton(onClick = onNextMonth) {
            Icon(painter = painterResource(R.drawable.ic_calendar_right), "다음 달")
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
    missionsCompleted: List<String>,
    onNavigateToDailyRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    val missionsCompletedSet = remember(missionsCompleted) {
        missionsCompleted.toSet()
    }

    Column {
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
                                .aspectRatio(3 / 4f)
                                .padding(4.dp),
                        )
                    } else if (dayIndex < daysInMonth) {
                        val date = yearMonth.atDay(dayIndex + 1)
                        val hasWalkSession = sessionsByDate[date]?.isNotEmpty() == true
                        val dateString =
                            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val hasMissionCompleted = missionsCompletedSet.contains(dateString)

                        CalendarDayCellRecord(
                            date = date,
                            day = dayIndex + 1,
                            hasWalkSession = hasWalkSession,
                            hasMissionCompleted = hasMissionCompleted,
                            onNavigateToDailyRecord = onNavigateToDailyRecord,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(3 / 4f)
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

        Spacer(Modifier.width(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(SemanticColor.stateGreenPrimary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "산책",
                // caption M/regular
                style = MaterialTheme.walkItTypography.captionM,
                color = SemanticColor.textBorderPrimary
            )
            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(SemanticColor.stateAquaBlueSecondary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "산책",
                // caption M/regular
                style = MaterialTheme.walkItTypography.captionM,
                color = SemanticColor.textBorderPrimary
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

/**
 * 캘린더 데이 셀 컴포넌트
 */
@Composable
private fun CalendarDayCellRecord(
    date: LocalDate,
    day: Int,
    hasWalkSession: Boolean,
    hasMissionCompleted: Boolean,
    onNavigateToDailyRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val isToday = date == today

    val backgroundColor =
        if (isToday) SemanticColor.stateAquaBlueTertiary else SemanticColor.backgroundWhitePrimary
    val borderColor = if (isToday) SemanticColor.stateAquaBluePrimary else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Ripple 효과는 선택 가능
                onClick = {
                    Log.d("CalendarDayCellRecord", "Clicked $date")
                    onNavigateToDailyRecord(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                }),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.walkItTypography.bodyS,
            color = Color(0xFF171717),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row() {
            // 산책 완료시 점
            if (hasWalkSession) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(SemanticColor.stateGreenPrimary)
                )
            }
            // 미션 완료시 점
            if (hasMissionCompleted) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(SemanticColor.stateAquaBluePrimary)
                )
            }

        }

        Spacer(modifier = Modifier.height(18.dp))
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
        //요일 헤더 - 날짜 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            weekDates.forEach { date ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
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
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (hasWalkSession) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_weekly_cell_walked),
                    contentDescription = "weekly day cell walked"
                )
            }

        } else {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
                    .background(
                        SemanticColor.textBorderDisabled
                    )
            ) {

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
    val emotionFrequency = monthSessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

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
 * 산책 시간 포맷팅 함수
 * 0시간보다 작으면 분으로 표시, 그 외에는 시간과 분으로 표시
 */
// FormatUtils로 통합됨 - formatWalkingTime은 FormatUtils.formatWalkingTime으로 대체

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
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "감정 기록",
                style = MaterialTheme.walkItTypography.bodyS,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
            )
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
                    // 산책 전 감정
                    EmotionCircleIcon(session.preWalkEmotion)
                    // 산책 후 감정
                    EmotionCircleIcon(session.postWalkEmotion)
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_more),
                            contentDescription = "더보기",
                            tint = SemanticColor.iconBlack
                        )
                    }

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

            HorizontalDivider(color = Color(0xFFF3F3F5), thickness = 1.dp)

            // 일기 내용
            val innerPadding = 10.dp
            SectionCard {
                if (isEditMode) {
                    BasicTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        textStyle = MaterialTheme.walkItTypography.captionM.copy(
                            color = SemanticColor.textBorderSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(138.dp)
                            .padding(10.dp) // 기존 높이 유지
                    )
                } else {
                    Text(
                        text = note.ifEmpty { "감정 일기 내용" },
                        style = MaterialTheme.walkItTypography.captionM,
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

@Composable
fun EmotionCircleIcon(emotion: EmotionType) {
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
        modifier = modifier.customShadow(),
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
                    text = "누적 산책 시간",
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
                text = text,
                style = MaterialTheme.walkItTypography.bodyS,
                color = currentTextColor
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
            preWalkEmotion = EmotionType.JOYFUL,
            postWalkEmotion = EmotionType.HAPPY,
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
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            if (showMenu) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(vertical = 4.dp)
                ) {
                    DropMenuItem(
                        text = "수정하기",
                        iconResId = R.drawable.ic_action_edit,
                        iconColor = Color(0xFF191919),
                        textColor = Color(0xFF191919),
                        backgroundColor = Color.White,
                        onClick = {}
                    )

                    DropMenuItem(
                        text = "삭제하기가 더 길어지면",
                        iconResId = R.drawable.ic_action_delete,
                        iconColor = Color(0xFF191919),
                        textColor = Color(0xFF191919),
                        backgroundColor = Color.White,
                        onClick = {}
                    )
                }
            }
        }
    }
}

/**
 * MonthSection Preview
 */
@Preview(showBackground = true, name = "MonthSection Preview")
@Composable
fun MonthSectionPreview() {
    val mockStats = WalkAggregate(
        steps = 45000,
        durationMillis = 7200000L, // 2시간
    )

    val mockSessions = listOf(
        WalkingSession(
            id = "1",
            startTime = System.currentTimeMillis() - 86400000L, // 1일 전
            endTime = System.currentTimeMillis() - 86400000L + 3600000L, // 1시간 산책
            stepCount = 2500,
            locations = emptyList(),
            totalDistance = 1800.0f,
            preWalkEmotion = EmotionType.HAPPY,
            postWalkEmotion = EmotionType.ANXIOUS,
            createdDate = "2024-01-01"
        ),
        WalkingSession(
            id = "2",
            startTime = System.currentTimeMillis() - 172800000L, // 2일 전
            endTime = System.currentTimeMillis() - 172800000L + 2700000L, // 45분 산책
            stepCount = 3200,
            locations = emptyList(),
            totalDistance = 2400.0f,
            preWalkEmotion = EmotionType.CONTENT,
            postWalkEmotion = EmotionType.HAPPY,
            createdDate = "2024-01-01"
        )
    )

    WalkItTheme {
        MonthSection(
            stats = mockStats,
            sessions = mockSessions,
            onNavigateToDailyRecord = {},
            onMonthChanged = {},
            missionsCompleted = emptyList()
        )
    }
}

/**
 * WeekSection Preview
 */
@Preview(showBackground = true, name = "WeekSection Preview")
@Composable
fun WeekSectionPreview() {
    val mockStats = WalkAggregate(
        steps = 1200,
        durationMillis = 123120459,
    )

    val mockSessions = listOf(
        WalkingSession(
            id = "1",
            startTime = System.currentTimeMillis() - 86400000L, // 1일 전
            endTime = System.currentTimeMillis() - 86400000L + 3600000L, // 1시간 산책
            stepCount = 2500,
            locations = emptyList(),
            totalDistance = 1800.0f,
            preWalkEmotion = EmotionType.HAPPY,
            postWalkEmotion = EmotionType.DEPRESSED,
            createdDate = "2024-01-01"
        ),
        WalkingSession(
            id = "2",
            startTime = System.currentTimeMillis() - 172800000L, // 2일 전
            endTime = System.currentTimeMillis() - 172800000L + 2700000L, // 45분 산책
            stepCount = 3200,
            locations = emptyList(),
            totalDistance = 2400.0f,
            preWalkEmotion = EmotionType.DEPRESSED,
            postWalkEmotion = EmotionType.HAPPY,
            createdDate = "2024-01-01"
        ),
        WalkingSession(
            id = "3",
            startTime = System.currentTimeMillis() - 259200000L, // 3일 전
            endTime = System.currentTimeMillis() - 259200000L + 1800000L, // 30분 산책
            stepCount = 2800,
            locations = emptyList(),
            totalDistance = 2000.0f,
            preWalkEmotion = EmotionType.TIRED,
            postWalkEmotion = EmotionType.HAPPY,
            createdDate = "2024-01-01"
        )
    )

    WalkItTheme {
        WeekSection(
            stats = mockStats,
            currentDate = LocalDate.now(),
            onPrevWeek = {},
            onNextWeek = {},
            sessions = mockSessions
        )
    }
}
