package swyp.team.walkit.ui.mypage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.record.components.cardBorder
import swyp.team.walkit.ui.record.components.customShadow
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 통계 섹션 컴포넌트 (재사용 가능)
 *
 * 두 가지 통계 정보를 나란히 표시합니다.
 * 예: 걸음 수와 산책 시간, 또는 다른 통계 조합
 *
 * @param leftLabel 왼쪽 통계 라벨 (예: "걸음 수")
 * @param leftValue 왼쪽 통계 값 (예: 걸음 수)
 * @param leftUnit 왼쪽 통계 단위 (예: "걸음")
 * @param rightLabel 오른쪽 통계 라벨 (예: "함께 걸은 시간")
 * @param rightValue 오른쪽 통계 값 (예: 시간(ms))
 * @param rightUnitFormatter 오른쪽 통계 단위 포맷터 (시간을 포맷하는 함수)
 * @param modifier Modifier
 */
@Composable
fun MyPageStatsSection(
    leftLabel: String = "걸음 수",
    leftValue: Int,
    leftUnit: String = "걸음",
    rightLabel: String = "함께 걸은 시간",
    rightValue: Long, // ms
    rightUnitFormatter: @Composable (Long) -> Unit = { timeMs ->
        // 기본 시간 포맷터 (시간과 분)
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = hours.toString(),
                style = MaterialTheme.walkItTypography.headingS,
                color = SemanticColor.textBorderPrimary,
            )
            Text(
                text = "시간",
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = SemanticColor.textBorderPrimary,
            )
            // 99시간 이상이면 분 표시하지 않음
            if (hours < 99) {
                Text(
                    text = minutes.toString(),
                    style = MaterialTheme.walkItTypography.headingS,
                    color = SemanticColor.textBorderPrimary,
                )
                Text(
                    text = "분",
                    style = MaterialTheme.walkItTypography.bodyM.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = SemanticColor.textBorderPrimary,
                )
            }
        }
    },
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.customShadow().cardBorder(),
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
            // 왼쪽 통계 섹션
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = leftLabel,
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = SemanticColor.textBorderPrimary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%,d".format(leftValue),
                        style = MaterialTheme.walkItTypography.headingS,
                        color = SemanticColor.textBorderPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = leftUnit,
                        style = MaterialTheme.walkItTypography.bodyM,
                        color = SemanticColor.textBorderPrimary,
                    )
                }
            }

            // 세로 구분선
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(SemanticColor.textBorderSecondaryInverse)
            )

            // 오른쪽 통계 섹션
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = rightLabel,
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = SemanticColor.textBorderPrimary,
                )
                rightUnitFormatter(rightValue)
            }
        }
    }
}

/**
 * 마이 페이지 통계 섹션 컴포넌트 (기존 호환성 유지)
 *
 * 누적 걸음수와 누적 산책 시간을 표시합니다.
 * 시간은 시간과 분으로 나뉘어 표시됩니다.
 *
 * @deprecated 이 함수는 호환성을 위해 유지됩니다. 
 *             새로운 코드에서는 MyPageStatsSection을 직접 사용하세요.
 */
@Composable
@Deprecated(
    message = "더 유연한 MyPageStatsSection을 사용하세요",
    replaceWith = ReplaceWith("MyPageStatsSection(leftValue = totalStepCount, rightValue = totalWalkingTime)")
)
fun MyPageStatsSection(
    totalStepCount: Int,
    totalWalkingTime: Long, // ms
    modifier: Modifier = Modifier,
) {
    MyPageStatsSection(
        leftLabel = "걸음 수",
        leftValue = totalStepCount,
        leftUnit = "걸음",
        rightLabel = "함께 걸은 시간",
        rightValue = totalWalkingTime,
        modifier = modifier
    )
}

@Preview(showBackground = true, name = "기본 케이스")
@Composable
private fun MyPageStatsSectionPreview() {
    WalkItTheme {
        MyPageStatsSection(
            leftValue = 12345,
            rightValue = 4 * 3600 * 1000L + 30 * 60 * 1000L, // 4시간 30분
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "99시간 59분")
@Composable
private fun MyPageStatsSection99HoursPreview() {
    WalkItTheme {
        MyPageStatsSection(
            leftValue = 912345,
            rightValue = (99 * 3600 + 59 * 60) * 1000L, // 99시간 59분
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "150시간 (100시간 이상)")
@Composable
private fun MyPageStatsSection150HoursPreview() {
    WalkItTheme {
        MyPageStatsSection(
            leftValue = 9000,
            rightValue = 150 * 3600 * 1000L, // 150시간
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "0걸음")
@Composable
private fun MyPageStatsSectionZeroStepsPreview() {
    WalkItTheme {
        MyPageStatsSection(
            leftValue = 0,
            rightValue = 2 * 3600 * 1000L, // 2시간
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "큰 숫자 걸음")
@Composable
private fun MyPageStatsSectionLargeStepsPreview() {
    WalkItTheme {
        MyPageStatsSection(
            leftValue = 912345,
            rightValue = 25 * 3600 * 1000L + 45 * 60 * 1000L, // 25시간 45분
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "커스텀 라벨")
@Composable
private fun MyPageStatsSectionCustomLabelPreview() {
    WalkItTheme {
        MyPageStatsSection(
            leftLabel = "오늘 걸음",
            leftValue = 8500,
            leftUnit = "걸음",
            rightLabel = "이번 주 시간",
            rightValue = 10 * 3600 * 1000L + 30 * 60 * 1000L, // 10시간 30분
            modifier = Modifier.padding(16.dp)
        )
    }
}







