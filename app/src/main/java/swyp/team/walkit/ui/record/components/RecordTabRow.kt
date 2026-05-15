package swyp.team.walkit.ui.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTypography
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 기록 탭 타입
 */
enum class RecordTabType {
    Week,
    Month,
}

/**
 * 기록 화면 탭 행 컴포넌트 (Custom)
 */
@Composable
fun RecordTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = RecordTabType.values()
    val containerShape = RoundedCornerShape(12.dp)
    val tabShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "감정 달력",
            style = MaterialTheme.walkItTypography.headingS.copy(
                fontWeight = FontWeight.SemiBold,
                color = SemanticColor.textBorderPrimary
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTabIndex == index

                Box(
                    modifier = Modifier
                        .clip(tabShape)
                        .border(
                            width = 1.dp,
                            color = if (selected) SemanticColor.stateGreenPrimary
                            else SemanticColor.textBorderTertiary,
                            shape = tabShape
                        )
                        .background(color = if (selected) SemanticColor.backgroundGreenPrimary else SemanticColor.backgroundWhitePrimary)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tab) {
                            RecordTabType.Week -> "주간"
                            RecordTabType.Month -> "월간"
                        },
                        color = if (selected)
                            SemanticColor.stateGreenPrimary
                        else
                            SemanticColor.textBorderPrimary,
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
