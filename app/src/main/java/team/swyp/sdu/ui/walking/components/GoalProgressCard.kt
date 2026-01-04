package team.swyp.sdu.ui.walking.components

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
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
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
    // 계산: 일주일 내 목표 달성한 세션 수
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val endOfWeek = startOfWeek.plusDays(6)

    // 이번 산책이 목표 달성했는지 확인
    val currentSessionAchieved = currentSession.stepCount >= goal.targetStepCount

    // 동기화된 세션 중 목표 달성한 세션 수 (과거 SYNC 기록)
    val achievedSyncedCount = syncedSessionsThisWeek.count { session ->
        session.stepCount >= goal.targetStepCount  // ✅ goal의 targetStepCount로 비교
    }

    // 일주일 전체 (7일)
    val targetWalkCount = goal.targetWalkCount

    // 과거 SYNC 기록의 진행률 (하얀색 점의 위치)
    val syncedProgressPercent =
        (achievedSyncedCount.toFloat() / targetWalkCount * 100).coerceIn(0f, 100f)

    // 전체 진행률 (과거 SYNC 기록 + 현재 산책)
    val totalProgressPercent = if (currentSessionAchieved) {
        val totalAchievedCount = achievedSyncedCount + 1
        (totalAchievedCount.toFloat() / targetWalkCount * 100).coerceIn(0f, 100f)
    } else {
        syncedProgressPercent
    }

    // 이번 산책으로 증가한 퍼센트
    val increasePercent = if (currentSessionAchieved) {
        totalProgressPercent - syncedProgressPercent
    } else {
        0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SemanticColor.backgroundWhitePrimary,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = SemanticColor.textBorderDisabled,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
        ) {
            // 제목 및 설명
            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "목표 진행률",
                    style = MaterialTheme.walkItTypography.bodyL,
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColor.textBorderPrimary,
                )

                if (increasePercent > 0f) {
                    Text(
                        text = "오늘 산책으로 목표에 ${increasePercent.toInt()}% 가까워졌어요!",
                        style = MaterialTheme.walkItTypography.bodyS,
                        fontWeight = FontWeight.SemiBold,
                        color = SemanticColor.statePinkPrimary,
                    )
                }
            }

            // 진행 바
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val progressBarWidth = maxWidth

                // 과거 SYNC 기록의 진행률 (하얀색 점 위치)
                val syncedProgressRatio = (syncedProgressPercent / 100f).coerceIn(0f, 1f)
                val syncedProgressWidthPx = syncedProgressRatio * progressBarWidth.value

                // 전체 진행률 (과거 SYNC + 현재 산책)
                val totalProgressRatio = (totalProgressPercent / 100f).coerceIn(0f, 1f)
                val totalProgressWidthPx = totalProgressRatio * progressBarWidth.value

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SemanticColor.backgroundWhiteTertiary),
                ) {
                    // 전체 진행률 바 (그라데이션) - 과거 SYNC 기록 + 현재 산책
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(totalProgressWidthPx.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        SemanticColor.stateGreenPrimary, // #52CE4B
                                        Color(0xFF22A04C), // #22A04C
                                    ),
                                ),
                            ),
                    )

                    // 하얀색 원 표시기 (과거 SYNC 기록의 끝 위치)
                    if (syncedProgressPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (syncedProgressWidthPx - 5).coerceAtLeast(0f).dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White),
                        )
                    }
                }
            }

            // 툴팁 (14% 증가) - Column의 다음 행으로 배치
            if (increasePercent > 0f) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp), // Column의 spacedBy(20.dp)를 상쇄하여 프로그래스 바와 툴팁 박스 사이를 정확히 20dp로
                ) {
                    val progressBarWidth = maxWidth

                    // 전체 진행률 (현재 산책으로 추가된 초록색의 끝)
                    val totalProgressRatio = (totalProgressPercent / 100f).coerceIn(0f, 1f)
                    val totalProgressWidthPx = totalProgressRatio * progressBarWidth.value

                    // 화살표가 가리켜야 할 위치: 전체 진행률의 끝 (오로지 진행률 퍼센트에만 의존)
                    val arrowTargetX =
                        totalProgressWidthPx.coerceIn(7.5f, progressBarWidth.value - 7.5f)

                    // 툴팁 박스 너비 추정 (한 줄로 고정, 여유 공간 포함)
                    val estimatedTooltipWidth = 80.dp.value
                    val minTooltipWidth = 60.dp.value
                    val padding = 0.dp.value // 좌우 여유 공간

                    // 툴팁 박스 위치 계산 (화살표와 독립적으로 배치)
                    var tooltipX = arrowTargetX - estimatedTooltipWidth / 2

                    // 화면 오른쪽 끝을 넘지 않도록 조정
                    val rightEdge = progressBarWidth.value - estimatedTooltipWidth - padding
                    if (tooltipX > rightEdge) {
                        tooltipX = rightEdge.coerceAtLeast(padding)
                    }

                    // 화면 왼쪽 끝을 넘지 않도록 보정
                    tooltipX = tooltipX.coerceAtLeast(padding)

                    // 최종적으로 툴팁이 화면 밖으로 나가지 않는지 확인
                    val tooltipRightEdge = tooltipX + estimatedTooltipWidth
                    if (tooltipRightEdge > progressBarWidth.value - padding) {
                        tooltipX =
                            (progressBarWidth.value - estimatedTooltipWidth - padding).coerceAtLeast(
                                padding
                            )
                    }

                    // 화살표와 툴팁을 독립적으로 배치
                    // 프로그래스 바 끝에서 툴팁 박스 시작까지 정확히 20dp가 되도록 배치
                    // 화살표 높이: 15dp, 화살표를 프로그래스 바 위 5dp 위치에 배치
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 화살표 (오로지 진행률 퍼센트에만 의존)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 5.dp), // 프로그래스 바 위 5dp
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Spacer(modifier = Modifier.width((arrowTargetX - 7.5f).dp))
                            Canvas(
                                modifier = Modifier.size(15.dp),
                            ) {
                                // 위를 향하는 삼각형 (뾰족한 부분이 위쪽)
                                val path = Path().apply {
                                    moveTo(size.width / 2, 0f) // 위쪽 뾰족한 부분
                                    lineTo(0f, size.height) // 왼쪽 아래
                                    lineTo(size.width, size.height) // 오른쪽 아래
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = SemanticColor.backgroundDarkPrimary,
                                )
                            }
                        }

                        // 툴팁 박스 (프로그래스 바 위 20dp 위치에 배치)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 20.dp), // 프로그래스 바 위 20dp
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Spacer(modifier = Modifier.width(tooltipX.dp))
                            Box(
                                modifier = Modifier
                                    .widthIn(min = minTooltipWidth.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SemanticColor.backgroundDarkPrimary)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "${increasePercent.toInt()}% 증가",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SemanticColor.textBorderPrimaryInverse,
                                    maxLines = 1,
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
            targetWalkCount = 5,
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


