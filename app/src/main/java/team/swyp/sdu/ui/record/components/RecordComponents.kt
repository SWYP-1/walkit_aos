package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.data.model.WalkingSession
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.ArrowForward
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import team.swyp.sdu.data.model.Emotion
import team.swyp.sdu.data.model.EmotionType

@Composable
fun HeaderRow(onDummyClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleAvatar(label = "ME", selected = true)
            repeat(3) { CircleAvatar(label = "", selected = false) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DummyButton(onClick = onDummyClick)
            IconButton(onClick = { /* 검색 */ }) {
                Icon(Icons.Default.Search, contentDescription = "검색")
            }
            IconButton(onClick = { /* 알림 */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "알림")
            }
        }
    }
}

@Composable
private fun DummyButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(40.dp),
    ) {
        Text(text = "더미 추가", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CircleAvatar(label: String, selected: Boolean) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFE0E0E0) else Color(0xFFD9D9D9)),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotBlank()) {
            Text(text = label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MonthSection(
    stats: WalkAggregate,
    sessions: List<WalkingSession>,
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val emotionsByDate = remember(sessions) {
        sessions.flatMap { session ->
            session.emotions.map { emotion ->
                val date = java.time.Instant.ofEpochMilli(emotion.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                date to emotion
            }
        }.groupBy({ it.first }, { it.second })
    }

    val monthlyStats = remember(sessions, currentMonth) {
        calculateMonthlyStatsForRecord(sessions, currentMonth, emotionsByDate)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MonthNavigator(
            currentMonth = currentMonth,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
        )

        CalendarGridRecord(
            yearMonth = currentMonth,
            emotionsByDate = emotionsByDate,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        EmotionCard(
            title = "이번달 주요 감정",
            emotion = monthlyStats.primaryMood,
            desc = monthlyStats.description,
        )

        StatsRow(
            listOf(
                StatItem("걸음 수", "%,d".format(monthlyStats.totalSteps)),
                StatItem("세션 수", "%,d".format(monthlyStats.sessionsCount)),
                StatItem("포커스", "${monthlyStats.focusScore} 점"),
            ),
        )
    }
}

@Composable
fun WeekSection(stats: WalkAggregate) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard {
            Text(text = "주간 캘린더", style = MaterialTheme.typography.titleMedium)
        }

        EmotionCard(
            title = "이번주 나의 주요 감정은?",
            emotion = "즐거움",
            desc = "즐거운 감정을 7일동안 4회 경험했어요!",
        )

        StatsRow(
            listOf(
                StatItem("걸음 수", "%,d".format(stats.steps)),
                StatItem("산책 시간", "${stats.durationHours}시간 ${stats.durationMinutesRemainder}분"),
            ),
        )

        GoalCheckRow()
    }
}

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

    LaunchedEffect(sessions.size) {
        // 새 데이터 들어오면 맨 앞으로
        scope.launch { listState.scrollToItem(0) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DateNavigator(
            dateLabel = dateLabel,
            onPrev = {
                onPrev()
                scope.launch { listState.scrollToItem(0) }
            },
            onNext = {
                onNext()
                scope.launch { listState.scrollToItem(0) }
            },
        )

        StatsRow(
            listOf(
                StatItem("산책 시간", "${stats.durationHours}시간 ${stats.durationMinutesRemainder}분"),
                StatItem("걸음 수", "%,d".format(stats.steps)),
                StatItem("총 거리", "— km"),
            ),
        )

        if (sessions.isEmpty()) {
            SectionCard(height = 160) {
                Text(text = "오늘 산책 기록이 없습니다", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(sessions) { index, session ->
                    SessionCard(session, index + 1, sessions.size)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: WalkingSession,
    position: Int,
    total: Int,
) {
    Card(
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "기록 $position/$total", style = MaterialTheme.typography.labelLarge)
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Text(
                text = "걸음 수: %,d".format(session.stepCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "소요 시간: ${((session.endTime ?: session.startTime) - session.startTime) / 60000}분",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "거리: %.2f km".format(session.totalDistance / 1000f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    height: Int = 260,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF1F1F1)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun EmotionCard(
    title: String,
    emotion: String,
    desc: String,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
        color = Color(0xFFF1F1F1),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = emotion,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = desc, style = MaterialTheme.typography.bodyMedium)
            }
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🙂", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

private data class StatItem(val title: String, val value: String)

@Composable
private fun StatsRow(items: List<StatItem>) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { item ->
            StatCard(title = item.title, value = item.value, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp)),
        color = Color(0xFFF1F1F1),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoalCheckRow() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
        color = Color(0xFFF1F1F1),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "이번주 목표 성공", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEachIndexed { index, label ->
                    val checked = index % 2 == 0
                    GoalDot(label = label, checked = checked)
                }
            }
        }
    }
}

@Composable
private fun GoalDot(label: String, checked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color(0xFF2C2C2C) else Color.White)
                    .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DateNavigator(
    dateLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "<",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable(onClick = onPrev),
        )
        Text(text = dateLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = ">",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable(onClick = onNext),
        )
    }
}

@Composable
private fun MonthNavigator(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "이전 달",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onNextMonth, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "다음 달",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarGridRecord(
    yearMonth: YearMonth,
    emotionsByDate: Map<LocalDate, List<Emotion>>,
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
                        val emotions = emotionsByDate[date] ?: emptyList()
                        val primaryEmotion = emotions.firstOrNull()

                        CalendarDayCellRecord(
                            day = dayIndex + 1,
                            emotion = primaryEmotion,
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
private fun CalendarDayCellRecord(
    day: Int,
    emotion: Emotion?,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, emoji) = getMoodColorAndEmojiRecord(emotion?.type)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = emotion != null) { },
        contentAlignment = Alignment.Center,
    ) {
        if (emotion != null) {
            Text(
                text = emoji,
                fontSize = 20.sp,
            )
        } else {
            Text(
                text = "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

private fun getMoodColorAndEmojiRecord(emotionType: EmotionType?): Pair<Color, String> =
    when (emotionType) {
        EmotionType.HAPPY -> Color(0xFFFFF59D) to "😊"
        EmotionType.JOYFUL -> Color(0xFFFFD54F) to "🤩"
        EmotionType.LIGHT_FOOTED -> Color(0xFFA5D6A7) to "😌"
        EmotionType.EXCITED -> Color(0xFFFFCC80) to "🤪"
        EmotionType.THRILLED -> Color(0xFFFFAB91) to "😃"
        EmotionType.TIRED -> Color(0xFFB0BEC5) to "😴"
        EmotionType.SAD -> Color(0xFFB3E5FC) to "😢"
        EmotionType.DEPRESSED -> Color(0xFF90A4AE) to "😞"
        EmotionType.SLUGGISH -> Color(0xFFC5CAE9) to "🥱"
        EmotionType.MANY_THOUGHTS -> Color(0xFFD1C4E9) to "🤔"
        EmotionType.COMPLEX_MIND -> Color(0xFFFFE082) to "🤯"
        EmotionType.CALM -> Color(0xFFAED581) to "😌"
        EmotionType.CONTENT -> Color(0xFF81C784) to "😄"
        EmotionType.ANXIOUS -> Color(0xFF80DEEA) to "😰"
        EmotionType.ENERGETIC -> Color(0xFFFFF176) to "⚡"
        EmotionType.RELAXED -> Color(0xFFA5D6A7) to "😊"
        EmotionType.PROUD -> Color(0xFF90CAF9) to "😎"
        null -> Color.White to "-"
    }

private data class MonthlyStatsRecord(
    val primaryMood: String,
    val description: String,
    val totalSteps: Int,
    val sessionsCount: Int,
    val focusScore: Int,
)

private fun calculateMonthlyStatsForRecord(
    sessions: List<WalkingSession>,
    month: YearMonth,
    emotionsByDate: Map<LocalDate, List<Emotion>>,
): MonthlyStatsRecord {
    val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthEnd = month.atEndOfMonth().atTime(23, 59, 59)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val monthSessions = sessions.filter { session ->
        session.startTime in monthStart..monthEnd
    }

    val totalSteps = monthSessions.sumOf { it.stepCount.toLong() }.toInt()
    val sessionsCount = monthSessions.size

    val emotionCounts = emotionsByDate.values.flatten()
        .filter { emotion ->
            val date = java.time.Instant.ofEpochMilli(emotion.timestamp)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            date.year == month.year && date.monthValue == month.monthValue
        }
        .groupBy { it.type }
        .mapValues { it.value.size }

    val primaryEmotionType = emotionCounts.maxByOrNull { it.value }?.key ?: EmotionType.HAPPY
    val primaryMood = primaryEmotionType.name

    val description = "이번달 주요 감정: $primaryMood"
    val focusScore = if (sessionsCount > 0) {
        ((sessionsCount.toFloat() / month.lengthOfMonth().toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else {
        0
    }

    return MonthlyStatsRecord(
        primaryMood = primaryMood,
        description = description,
        totalSteps = totalSteps,
        sessionsCount = sessionsCount,
        focusScore = focusScore,
    )
}
