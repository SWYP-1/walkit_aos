package swyp.team.walkit.ui.record.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.ui.theme.WalkItTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

// ─────────────────────────────────────────────────────────────────────────────
// 목표달성률 차트 Preview — 목표 3000보/회, 3회/주, 실제 걸음 4000보 기준
// ─────────────────────────────────────────────────────────────────────────────

private val previewGoal = Goal(targetStepCount = 3000, targetWalkCount = 3)

private fun previewWeekDates(): List<LocalDate> {
    val monday = LocalDate.now().with(DayOfWeek.MONDAY)
    return (0..6).map { monday.plusDays(it.toLong()) }
}

private fun fakeSession(date: LocalDate, stepCount: Int, emotion: EmotionType): WalkingSession {
    val startTime = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return WalkingSession(
        startTime = startTime,
        endTime = startTime + 3_600_000L,
        stepCount = stepCount,
        preWalkEmotion = emotion.name,
        postWalkEmotion = emotion.name,
        createdDate = date.toString(),
    )
}

// ── 1회: 월요일만 달성 ────────────────────────────────────────────────────────
@Preview(name = "Goal Chart – 1회", showBackground = true)
@Composable
private fun PreviewGoalChart1() {
    val d = previewWeekDates()
    WalkItTheme {
        WeeklyGoalBarChartCard(
            weekDates = d,
            sessionsByDate = mapOf(d[0] to listOf(fakeSession(d[0], 4000, EmotionType.HAPPY))),
            goal = previewGoal,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

// ── 3회: 월·수·금 모두 달성 ──────────────────────────────────────────────────
@Preview(name = "Goal Chart – 3회 달성", showBackground = true)
@Composable
private fun PreviewGoalChart3() {
    val d = previewWeekDates()
    WalkItTheme {
        WeeklyGoalBarChartCard(
            weekDates = d,
            sessionsByDate = mapOf(
                d[0] to listOf(fakeSession(d[0], 4000, EmotionType.HAPPY)),
                d[2] to listOf(fakeSession(d[2], 4000, EmotionType.JOYFUL)),
                d[4] to listOf(fakeSession(d[4], 4000, EmotionType.DELIGHTED)),
            ),
            goal = previewGoal,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

// ── 5회: 3회 달성 + 2회 미달 혼합 ────────────────────────────────────────────
@Preview(name = "Goal Chart – 5회 혼합", showBackground = true)
@Composable
private fun PreviewGoalChart5() {
    val d = previewWeekDates()
    WalkItTheme {
        WeeklyGoalBarChartCard(
            weekDates = d,
            sessionsByDate = mapOf(
                d[0] to listOf(fakeSession(d[0], 4000, EmotionType.HAPPY)),
                d[1] to listOf(fakeSession(d[1], 1800, EmotionType.TIRED)),       // 미달
                d[2] to listOf(fakeSession(d[2], 4000, EmotionType.JOYFUL)),
                d[4] to listOf(fakeSession(d[4], 2400, EmotionType.DEPRESSED)),   // 미달
                d[5] to listOf(fakeSession(d[5], 4000, EmotionType.DELIGHTED)),
            ),
            goal = previewGoal,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

// ── 7회: 매일 달성 ────────────────────────────────────────────────────────────
@Preview(name = "Goal Chart – 7회 전부 달성", showBackground = true)
@Composable
private fun PreviewGoalChart7() {
    val d = previewWeekDates()
    val emotions = EmotionType.values()
    WalkItTheme {
        WeeklyGoalBarChartCard(
            weekDates = d,
            sessionsByDate = d.mapIndexed { i, date ->
                date to listOf(fakeSession(date, 4000, emotions[i % emotions.size]))
            }.toMap(),
            goal = previewGoal,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

// ── 8회: 월요일 2회 포함 ──────────────────────────────────────────────────────
@Preview(name = "Goal Chart – 8회 (월 2회)", showBackground = true)
@Composable
private fun PreviewGoalChart8() {
    val d = previewWeekDates()
    WalkItTheme {
        WeeklyGoalBarChartCard(
            weekDates = d,
            sessionsByDate = mapOf(
                d[0] to listOf(
                    fakeSession(d[0], 4000, EmotionType.HAPPY),
                    fakeSession(d[0], 3500, EmotionType.JOYFUL),   // 동일 날 2회 달성
                ),
                d[1] to listOf(fakeSession(d[1], 4000, EmotionType.DELIGHTED)),
                d[2] to listOf(fakeSession(d[2], 1500, EmotionType.TIRED)),        // 미달
                d[3] to listOf(fakeSession(d[3], 4000, EmotionType.IRRITATED)),
                d[4] to listOf(fakeSession(d[4], 4000, EmotionType.DEPRESSED)),
                d[5] to listOf(fakeSession(d[5], 4000, EmotionType.HAPPY)),
                d[6] to listOf(fakeSession(d[6], 4000, EmotionType.JOYFUL)),
            ),
            goal = previewGoal,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
