package swyp.team.walkit.ui.walking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 목표 진행률 카드 컴포넌트
 *
 * 일주일 내 목표 달성 세션의 비율을 표시합니다.
 *
 * @param goal 목표 정보 (목표 걸음 수)
 * @param currentSession 현재 산책 세션
 * @param syncedSessionsThisWeek 일주일 내 동기화된 세션 목록 (SYNCED 상태만)
 * @param modifier Modifier
 */
@Composable
fun GoalProgressCard(
    goal: Goal,
    currentSession: WalkingSession,
    syncedSessionsThisWeek: List<WalkingSession>,
    modifier: Modifier = Modifier,
) {
    val currentSessionAchieved = currentSession.stepCount >= goal.targetStepCount
    val achievedSyncedCount = syncedSessionsThisWeek.count {
        it.stepCount >= goal.targetStepCount
    }

    val targetWalkCount = goal.targetWalkCount

    val syncedProgressPercent =
        (achievedSyncedCount.toFloat() / targetWalkCount * 100).coerceIn(0f, 100f)

    val totalProgressPercent = if (currentSessionAchieved) {
        ((achievedSyncedCount + 1).toFloat() / targetWalkCount * 100).coerceIn(0f, 100f)
    } else {
        syncedProgressPercent
    }

    val increasePercent = if (currentSessionAchieved) {
        totalProgressPercent - syncedProgressPercent
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SemanticColor.backgroundWhitePrimary
        ),
        border = BorderStroke(1.dp, SemanticColor.textBorderDisabled),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            /* ---------- Title ---------- */
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "목표 진행률",
                    style = MaterialTheme.walkItTypography.bodyL,
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColor.textBorderPrimary
                )

                if (increasePercent > 0f) {
                    Text(
                        text = "오늘 산책으로 목표에 ${increasePercent.toInt()}% 가까워졌어요!",
                        style = MaterialTheme.walkItTypography.bodyS,
                        fontWeight = FontWeight.SemiBold,
                        color = SemanticColor.statePinkPrimary
                    )
                }
            }

            /* ---------- Progress Bar ---------- */
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val barWidth = maxWidth.value

                val totalRatio = (totalProgressPercent / 100f).coerceIn(0f, 1f)
                val syncedRatio = (syncedProgressPercent / 100f).coerceIn(0f, 1f)

                val totalPx = barWidth * totalRatio
                val syncedPx = barWidth * syncedRatio

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SemanticColor.backgroundWhiteTertiary)
                ) {

                    // 전체 진행률
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(totalPx.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        SemanticColor.stateGreenPrimary,
                                        Color(0xFF22A04C)
                                    )
                                )
                            )
                    )

                    // 과거 SYNC 표시 점
                    if (syncedProgressPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (syncedPx - 5).coerceAtLeast(0f).dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        )
                    }
                }
            }

            /* ---------- Tooltip + Arrow (핵심 수정 영역) ---------- */
            if (increasePercent > 0f) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                ) {
                    val barWidth = maxWidth.value

                    val tooltipWidth = 80f
                    val arrowHalf = 7.5f

                    val totalRatio = (totalProgressPercent / 100f).coerceIn(0f, 1f)
                    val totalPx = barWidth * totalRatio

                    // ✅ 툴팁 X 먼저 확정
                    val tooltipX = (totalPx - tooltipWidth / 2)
                        .coerceIn(0f, barWidth - tooltipWidth)

                    // ✅ 화살표는 항상 툴팁 중앙
                    val arrowX = tooltipX + tooltipWidth / 2

                    Box(modifier = Modifier.fillMaxWidth()) {

                        /* ▲ Arrow */
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 5.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Spacer(Modifier.width((arrowX - arrowHalf).dp))
                            Canvas(Modifier.size(15.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2, 0f)
                                    lineTo(0f, size.height)
                                    lineTo(size.width, size.height)
                                    close()
                                }
                                drawPath(path, SemanticColor.backgroundDarkPrimary)
                            }
                        }

                        /* Tooltip */
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 20.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = tooltipX.dp) // ← 위치만 이동
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SemanticColor.backgroundDarkPrimary)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${increasePercent.toInt()}% 증가",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SemanticColor.textBorderPrimaryInverse,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GoalProgressCardPreview() {
    WalkItTheme {
        val goal = Goal(
            targetStepCount = 10000,
            targetWalkCount = 2,
        )

        val currentSession = WalkingSession(
            id = "preview-session",
            startTime = System.currentTimeMillis() - 3600000, // 1시간 전
            endTime = System.currentTimeMillis(),
            stepCount = 12000, // 목표 달성
            locations = emptyList(),
            totalDistance = 5000f,
            preWalkEmotion = "JOYFUL",
            postWalkEmotion = "JOYFUL",
            note = null,
            createdDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        )

        // 일주일 내 동기화된 세션 목록 (목표 달성한 세션 2개)
        val syncedSessionsThisWeek = listOf(
            WalkingSession(
                id = "session-1",
                startTime = System.currentTimeMillis() - 86400000 * 2, // 2일 전
                endTime = System.currentTimeMillis() - 86400000 * 2 + 3600000,
                stepCount = 11000, // 목표 달성
                locations = emptyList(),
                totalDistance = 4500f,
                preWalkEmotion = "JOYFUL",
                postWalkEmotion = "JOYFUL",
                note = null,
                createdDate = ZonedDateTime.now().minusDays(2)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
            ),
            WalkingSession(
                id = "session-2",
                startTime = System.currentTimeMillis() - 86400000 * 3, // 3일 전
                endTime = System.currentTimeMillis() - 86400000 * 3 + 3600000,
                stepCount = 8000, // 목표 미달성
                locations = emptyList(),
                totalDistance = 3500f,
                preWalkEmotion = "JOYFUL",
                postWalkEmotion = "JOYFUL",
                note = null,
                createdDate = ZonedDateTime.now().minusDays(3)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
            ),
        )

        GoalProgressCard(
            goal = goal,
            currentSession = currentSession,
            syncedSessionsThisWeek = syncedSessionsThisWeek,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "목표 미달성")
@Composable
fun GoalProgressCardNotAchievedPreview() {
    WalkItTheme {
        val goal = Goal(
            targetStepCount = 10000,
            targetWalkCount = 5,
        )

        val currentSession = WalkingSession(
            id = "preview-session",
            startTime = System.currentTimeMillis() - 3600000,
            endTime = System.currentTimeMillis(),
            stepCount = 8000, // 목표 미달성
            locations = emptyList(),
            totalDistance = 4000f,
            preWalkEmotion = "JOYFUL",
            postWalkEmotion = "JOYFUL",
            note = null,
            createdDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        )

        val syncedSessionsThisWeek = emptyList<WalkingSession>()

        GoalProgressCard(
            goal = goal,
            currentSession = currentSession,
            syncedSessionsThisWeek = syncedSessionsThisWeek,
            modifier = Modifier.padding(16.dp),
        )
    }
}


