package team.swyp.sdu.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import team.swyp.sdu.data.model.Emotion
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.WalkingSessionListViewModel
import team.swyp.sdu.utils.NumberUtils

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

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }


    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val monthlyStats = remember(sessions, currentMonth) {
        calculateMonthlyStats(sessions, currentMonth)
    }

    val navigationBarsPadding = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.generateNovemberTestData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CalendarHeader(
            currentMonth = currentMonth,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
            onSearchClick = { /* TODO: 검색 기능 */ },
        )

        Spacer(modifier = Modifier.height(8.dp))

        CalendarGrid(
            yearMonth = currentMonth,
            sessionsByDate = sessionsByDate,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        MonthlyMoodSummary(
            primaryMood = monthlyStats.primaryMood,
            description = monthlyStats.description,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatisticsCards(
            totalSteps = monthlyStats.totalSteps,
            sessionsCount = monthlyStats.sessionsCount,
            focusScore = monthlyStats.focusScore,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { /* TODO: 뒤로가기 처리 */ },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mood Calendar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = currentMonth.format(
                    DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.ENGLISH),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "이전 달",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "다음 달",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    sessionsByDate: Map<LocalDate, List<team.swyp.sdu.data.model.WalkingSession>>,
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
                    textAlign = TextAlign.Center,
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

                        CalendarDayCell(
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

@Composable
private fun CalendarDayCell(
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

// 커스텀 캘린더 데이 셀을 위한 확장 함수
@Composable
fun CustomCalendarDayCell(
    day: Int,
    emotion: Emotion?,
    hasWalkSession: Boolean = false,
    modifier: Modifier = Modifier,
    backgroundColor: Color = getMoodColorAndEmoji(emotion?.type).first,
    contentColor: Color = if (emotion != null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    },
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
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
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier.clickable(enabled = emotion != null) { /* 기본 클릭 처리 */ }
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MonthlyMoodSummary(
    primaryMood: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF97FFB5),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Monthly Mood Summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = primaryMood,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    lineHeight = 22.sp,
                )
            }
            Text(
                text = getMoodEmoji(primaryMood),
                fontSize = 72.sp,
            )
        }
    }
}

@Composable
private fun StatisticsCards(
    totalSteps: Int,
    sessionsCount: Int,
    focusScore: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatisticCard(
            title = "Activity",
            value = NumberUtils.formatNumber(totalSteps),
            unit = "Steps",
            modifier = Modifier.weight(1f),
        )
        StatisticCard(
            title = "Therapy",
            value = "$sessionsCount/30",
            unit = "Sessions",
            modifier = Modifier.weight(1f),
        )
        StatisticCard(
            title = "Discipline",
            value = "$focusScore%",
            unit = "Focus score",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
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
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun getMoodColorAndEmoji(emotionType: EmotionType?): Pair<Color, String> =
    when (emotionType) {
        EmotionType.HAPPY -> Color(0xFFFFE082) to "😊"
        EmotionType.JOYFUL -> Color(0xFFFFE082) to "🤩"
        EmotionType.CONTENT -> Color(0xFF90CAF9) to "😄"
        EmotionType.DEPRESSED -> Color(0xFF90CAF9) to "😔"
        EmotionType.TIRED -> Color(0xFFCE93D8) to "😴"
        EmotionType.ANXIOUS -> Color(0xFFF48FB1) to "😰"
        null -> Color.White to "-"
    }

private fun getMoodEmoji(mood: String): String =
    when (mood.lowercase()) {
        "happy" -> "😊"
        "excited" -> "🤩"
        "calm" -> "😌"
        "content" -> "😄"
        "tired" -> "😴"
        "sad" -> "😢"
        "anxious" -> "😰"
        "energetic" -> "⚡"
        "relaxed" -> "😊"
        "proud" -> "😎"
        else -> "😊"
    }

private data class MonthlyStats(
    val primaryMood: String,
    val description: String,
    val totalSteps: Int,
    val sessionsCount: Int,
    val focusScore: Int,
)

private fun calculateMonthlyStats(
    sessions: List<WalkingSession>,
    month: YearMonth,
): MonthlyStats {
    val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthEnd = month.atEndOfMonth().atTime(23, 59, 59)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val monthSessions = sessions.filter { session ->
        session.startTime in monthStart..monthEnd
    }

    val totalSteps = monthSessions.sumOf { it.stepCount.toLong() }.toInt()
    val sessionsCount = monthSessions.size

    // 감정 관련 로직 제거됨 - 기본값 설정
    val primaryMood = "Walking"
    val description = "You've been walking regularly. Keep up the good work!"

    val focusScore = if (sessionsCount > 0) {
        ((sessionsCount.toFloat() / 30f) * 100f).toInt().coerceIn(0, 100)
    } else {
        0
    }

    return MonthlyStats(
        primaryMood = primaryMood,
        description = description,
        totalSteps = totalSteps,
        sessionsCount = sessionsCount,
        focusScore = focusScore,
    )
}



