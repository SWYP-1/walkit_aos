package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.Pretendard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.TypeScale
import java.text.DecimalFormat

/**
 * 요약 카드의 단위 타입
 * 
 * 각 타입별로 포맷팅 로직을 포함합니다.
 */
sealed class SummaryUnit {
    /**
     * 걸음 수 단위
     */
    data class Step(val unit: String) : SummaryUnit()
    
    /**
     * 시간 단위 (밀리초를 시/분으로 포맷팅)
     * 예: 3660000ms -> "1시간 1분"
     */
    data class Time(val durationMs: Long) : SummaryUnit()
    
    /**
     * 거리 단위 (미터를 km/m으로 포맷팅)
     * 예: 5200.0m -> "5.2" + "km"
     */
    data class Distance(val distanceMeters: Float) : SummaryUnit()
    
    /**
     * 포맷팅된 값과 단위를 반환
     * @return Pair<값, 단위> (단위가 null이면 표시하지 않음)
     */
    fun format(): Pair<String, String?> {
        return when (this) {
            is Step -> Pair("", unit)
            is Time -> {
                val totalSeconds = durationMs / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                
                val value = when {
                    hours > 0 -> "${hours}시간 ${minutes}분"
                    minutes > 0 -> "${minutes}분"
                    else -> "0분"
                }
                Pair(value, null) // 시간은 값에 단위가 포함되어 있으므로 unit은 null
            }
            is Distance -> {
                val (value, unit) = if (distanceMeters >= 1000) {
                    Pair(String.format("%.1f", distanceMeters / 1000f), "km")
                } else {
                    Pair(DecimalFormat("#,###").format(distanceMeters.toInt()), "m")
                }
                Pair(value, unit)
            }
        }
    }
}

/**
 * 범용 요약 카드 컴포넌트
 *
 * 두 개의 값을 나란히 표시하는 카드입니다.
 * 산책 기록, 마이페이지 통계 등 다양한 용도로 사용 가능합니다.
 *
 * @param leftLabel 왼쪽 섹션 라벨
 * @param leftValue 왼쪽 섹션 값 (포맷된 문자열)
 * @param leftUnit 왼쪽 섹션 단위 (선택사항, SummaryUnit.Step 사용)
 * @param rightLabel 오른쪽 섹션 라벨
 * @param rightUnit 오른쪽 섹션 단위 (SummaryUnit 사용, 시간/거리 자동 포맷팅)
 * @param rightValue 오른쪽 섹션 값 (rightUnit이 Step인 경우에만 사용)
 * @param modifier Modifier
 * @param onClick 클릭 이벤트 핸들러 (선택사항)
 * @param header 헤더 컴포저블 (선택사항, null이면 표시되지 않음)
 */
@Composable
fun WalkingSummaryCard(
    leftLabel: String,
    leftValue: String,
    leftUnit: SummaryUnit.Step? = null,
    rightLabel: String,
    rightUnit: SummaryUnit,
    rightValue: String = "", // rightUnit이 Step인 경우에만 사용
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 16.dp
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color(0x0F000000), // Shadow-1: rgba(0,0,0,0.06) with spread 7
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF), // color/background/whtie-primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // shadow로 처리
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 헤더 (있는 경우만 표시)
            header?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = cardHorizontalPadding, top = cardVerticalPadding)
                ) {
                    it()
                }
            }

            // 메인 콘텐츠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardHorizontalPadding, vertical = cardVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 왼쪽 섹션
                SummarySection(
                    label = leftLabel,
                    value = leftValue,
                    unit = leftUnit?.unit,
                )

                // 세로 구분선
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(SemanticColor.textBorderSecondaryInverse), // color/text-border/secondary-inverse
                )

                Spacer(Modifier.width(cardHorizontalPadding))

                // 오른쪽 섹션
                val (rightFormattedValue, rightFormattedUnit) = when (rightUnit) {
                    is SummaryUnit.Step -> Pair(rightValue.ifEmpty { "" }, rightUnit.unit)
                    else -> rightUnit.format()
                }
                SummarySection(
                    label = rightLabel,
                    value = rightFormattedValue,
                    unit = rightFormattedUnit,
                )
            }
        }
    }
}

/**
 * 요약 섹션 (라벨 + 값 + 단위)
 */
@Composable
private fun RowScope.SummarySection(
    label: String,
    value: String,
    unit: String?,
) {
    Column(
        modifier = Modifier.weight(1f),
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
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
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

            // 단위 (있는 경우만 표시)
            unit?.let {
                Text(
                    text = it,
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
}

/**
 * 걸음 수를 포맷팅 (천 단위 구분자 추가)
 * 예: 18312 -> "18,312"
 */
fun formatStepCount(stepCount: Int): String {
    val formatter = DecimalFormat("#,###")
    return formatter.format(stepCount)
}

/**
 * 시간을 "-시간 -분" 형식으로 포맷팅
 * 예: 3660000ms (61분) -> "1시간 1분"
 * 예: 1800000ms (30분) -> "0시간 30분"
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        "${hours}시간 ${minutes}분"
    } else {
        "0시간 ${minutes}분"
    }
}

/**
 * 거리를 포맷팅 (km 또는 m)
 * 예: 5200.0m -> "5.2"
 * 예: 500.0m -> "500"
 */
fun formatDistance(distanceMeters: Float): String {
    return if (distanceMeters >= 1000) {
        String.format("%.1f", distanceMeters / 1000f)
    } else {
        DecimalFormat("#,###").format(distanceMeters.toInt())
    }
}

