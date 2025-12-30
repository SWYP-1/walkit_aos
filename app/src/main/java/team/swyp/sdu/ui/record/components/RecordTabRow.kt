package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.background
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
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 기록 탭 타입
 */
enum class RecordTabType {
    Month,
    Week,
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
    val tabs = RecordTabType.entries
    val containerShape = RoundedCornerShape(12.dp)
    val tabShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .customShadow()
            .background(
                color = SemanticColor.backgroundWhitePrimary,
                shape = containerShape
            )
            .padding(vertical = 8.dp, horizontal = 7.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTabIndex == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(tabShape)
                        .background(
                            color = if (selected)
                                SemanticColor.stateAquaBluePrimary
                            else
                                Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tab) {
                            RecordTabType.Month -> "월간"
                            RecordTabType.Week -> "주간"
                        },
                        color = if (selected)
                            SemanticColor.textBorderPrimaryInverse
                        else
                            SemanticColor.textBorderSecondary,
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
