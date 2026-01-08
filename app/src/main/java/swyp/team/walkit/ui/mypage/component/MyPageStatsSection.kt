package swyp.team.walkit.ui.mypage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swyp.team.walkit.ui.record.components.cardBorder
import swyp.team.walkit.ui.record.components.customShadow
import swyp.team.walkit.ui.theme.Pretendard
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.TypeScale
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.utils.FormatUtils.formatStepCount

/**
 * 마이 페이지 통계 섹션 컴포넌트
 *
 * 누적 걸음수와 누적 산책 시간을 표시합니다.
 * 시간은 시간과 분으로 나뉘어 표시됩니다.
 */
@Composable
fun MyPageStatsSection(
    totalStepCount: Int,
    totalWalkingTime: Long, // ms
    modifier: Modifier = Modifier,
) {
    val cardPadding = 16.dp

    // 시간 계산
    val totalSeconds = totalWalkingTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    Box(
        modifier = modifier
            .customShadow()
            .clip(RoundedCornerShape(12.dp))
            .background(SemanticColor.backgroundWhitePrimary)
            .cardBorder()
            .padding(cardPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /* ---------- 왼쪽 : 누적 걸음수 (50%) ---------- */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                TimeSummarySection(
                    label = "걸음수",
                    value = formatStepCount(totalStepCount),
                    unit = "걸음"
                )
            }

            /* ---------- 중앙 Divider (정확히 중앙) ---------- */
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp) // divider 좌우 여백
                    .width(1.dp)
                    .height(40.dp)
                    .background(SemanticColor.textBorderSecondaryInverse)
            )

            /* ---------- 오른쪽 : 누적 산책 시간 (50%) ---------- */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {

                    Text(
                        text = "함께 걸은 시간",
                        style = MaterialTheme.walkItTypography.captionM,
                        color = SemanticColor.textBorderPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (hours >= 100) {
                            TimeValueSection(
                                value = hours.toString(),
                                unit = "시간"
                            )
                        } else {
                            TimeValueSection(
                                value = hours.toString(),
                                unit = "시간"
                            )
                            TimeValueSection(
                                value = minutes.toString(),
                                unit = "분"
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * 시간 요약 섹션 (라벨 + 값 + 단위)
 * 전체 카드의 50%를 차지하는 섹션
 */
@Composable
private fun TimeSummarySection(
    label: String,
    value: String,
    unit: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // 라벨
        Text(
            text = label,
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.Normal
            ),
            color = SemanticColor.textBorderPrimary
        )

        // 값 + 단위
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.walkItTypography.headingS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = unit,
                fontFamily = Pretendard,
                fontSize = TypeScale.BodyM,
                fontWeight = FontWeight.Normal,
                lineHeight = (TypeScale.BodyM.value * 1.5f).sp,
                letterSpacing = (-0.16f).sp,
                color = Color(0xFF191919)
            )
        }
    }
}


/**
 * 시간 값 섹션 (값 + 단위만 표시 - AnnotatedString 사용)
 * 누적 산책 시간 영역 내에서 시간/분 각각 50%를 차지
 */
@Composable
private fun TimeValueSection(
    value: String,
    unit: String,
) {
    Text(
        text = buildAnnotatedString {
            append(value)
            withStyle(
                SpanStyle(
                    fontSize = MaterialTheme.walkItTypography.bodyS.fontSize
                )
            ) { append(" $unit") }
        },
        style = MaterialTheme.walkItTypography.headingS.copy(
            fontWeight = FontWeight.Medium
        )
    )
}






