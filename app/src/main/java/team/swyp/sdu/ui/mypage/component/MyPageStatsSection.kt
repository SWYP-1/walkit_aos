package team.swyp.sdu.ui.mypage.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.components.SummaryUnit
import team.swyp.sdu.ui.components.WalkingSummaryCard
import team.swyp.sdu.ui.components.formatStepCount

/**
 * 마이 페이지 통계 섹션 컴포넌트
 *
 * 누적 걸음수와 이동거리를 표시합니다.
 */
@Composable
fun MyPageStatsSection(
    totalStepCount: Int,
    totalDistanceKm: String,
    modifier: Modifier = Modifier,
) {
    WalkingSummaryCard(
        leftLabel = "누적 걸음수",
        leftValue = formatStepCount(totalStepCount),
        leftUnit = SummaryUnit.Step("걸음"),
        rightLabel = "누적 이동거리",
        rightValue = totalDistanceKm,
        rightUnit = SummaryUnit.Step("km"),
        modifier = modifier.padding(horizontal = 20.dp),
    )
}





