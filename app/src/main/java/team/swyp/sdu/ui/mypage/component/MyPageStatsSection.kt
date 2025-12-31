package team.swyp.sdu.ui.mypage.component

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.Pretendard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.TypeScale
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.FormatUtils.formatStepCount

/**
 * 마이 페이지 통계 섹션 컴포넌트
 *
 * 누적 걸음수와 누적 산책 시간을 표시합니다.
 * 시간은 시간과 분으로 나뉘어 표시됩니다.
 */
@Composable
fun MyPageStatsSection(
    totalStepCount: Int,
    totalWalkingTime: Long, // 밀리초
    modifier: Modifier = Modifier,
) {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 16.dp

    // 시간과 분 계산
    val totalSeconds = totalWalkingTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color(0x0F000000),
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardHorizontalPadding, vertical = cardVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽 섹션: 누적 걸음수 (50%)
            TimeSummarySection(
                label = "누적 걸음수",
                value = formatStepCount(totalStepCount),
                unit = "걸음",
            )

            // 세로 구분선
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(SemanticColor.textBorderSecondaryInverse),
            )

            Spacer(Modifier.width(cardHorizontalPadding))

            // 오른쪽 섹션: 누적 산책 시간 (50%)
            // 시간과 분을 포함하는 컨테이너
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // 라벨: 누적 산책 시간
                Text(
                    text = "누적 산책 시간",
                    style = MaterialTheme.walkItTypography.captionM,
                    color = SemanticColor.textBorderPrimary, // color/text-border/primary
                )

                Spacer(Modifier.height(2.dp))

                // 시간과 분을 나란히 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 시간 섹션 (50%)
                    TimeValueSection(
                        value = hours.toString(),
                        unit = "시간",
                    )
                    Spacer(Modifier.width(cardHorizontalPadding))
                    // 분 섹션 (50%)
                    TimeValueSection(
                        value = minutes.toString(),
                        unit = "분",
                    )
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
private fun RowScope.TimeSummarySection(
    label: String,
    value: String,
    unit: String,
) {
    Column(
        modifier = Modifier.padding(end = 20.dp).weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // 라벨
        Text(
            text = label,
            fontFamily = Pretendard,
            fontSize = TypeScale.CaptionM, // 12sp
            fontWeight = FontWeight.Normal, // Regular
            lineHeight = (TypeScale.CaptionM.value * 1.3f).sp, // lineHeight 1.3
            letterSpacing = (-0.12f).sp, // letterSpacing -0.12px
            color = Color(0xFF191919), // color/text-border/primary
        )

        // 값 + 단위
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 값
            Text(
                text = value,
                fontFamily = Pretendard,
                fontSize = TypeScale.HeadingS, // 22sp
                fontWeight = FontWeight.Medium, // Medium
                lineHeight = (TypeScale.HeadingS.value * 1.5f).sp, // lineHeight 1.5
                letterSpacing = (-0.22f).sp, // letterSpacing -0.22px
                color = Color(0xFF191919), // color/text-border/primary
            )
            Spacer(Modifier.width(4.dp))

            // 단위
            Text(
                text = unit,
                fontFamily = Pretendard,
                fontSize = TypeScale.BodyM, // 16sp
                fontWeight = FontWeight.Normal, // Regular
                lineHeight = (TypeScale.BodyM.value * 1.5f).sp, // lineHeight 1.5
                letterSpacing = (-0.16f).sp, // letterSpacing -0.16px
                color = Color(0xFF191919), // color/text-border/primary
            )
        }
    }
}

/**
 * 시간 값 섹션 (값 + 단위만 표시 - AnnotatedString 사용)
 * 누적 산책 시간 영역 내에서 시간/분 각각 50%를 차지
 */
@Composable
private fun RowScope.TimeValueSection(
    value: String,
    unit: String,
) {
    Text(
        text = buildAnnotatedString {
            append(value)
            withStyle(
                SpanStyle(
                    fontSize = MaterialTheme.walkItTypography.bodyS.fontSize,
                    fontWeight = FontWeight.Normal,
                    color = SemanticColor.textBorderPrimary
                )
            ) {
                append(" $unit")
            }
        },
        style = MaterialTheme.walkItTypography.headingS.copy(
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.weight(1f)
    )
}





