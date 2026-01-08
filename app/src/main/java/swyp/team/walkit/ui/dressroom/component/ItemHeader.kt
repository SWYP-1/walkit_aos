package swyp.team.walkit.ui.dressroom.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.domain.model.EquipSlot
import swyp.team.walkit.ui.components.CustomSwitch
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.createWalkItTypography
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun ItemHeader(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // 카테고리 필터 (CharacterShop에서만 사용)
    selectedCategory: EquipSlot? = null,
    onCategoryFilterChange: ((EquipSlot?) -> Unit)? = null,
    showCategoryFilter: Boolean = false
) {
    Column(
        modifier = Modifier
            .background(
                color = SemanticColor.backgroundWhitePrimary,
            )
            .fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .background(SemanticColor.backgroundWhiteQuaternary)
            ) {

            }
        }
        Spacer(Modifier.height(16.dp))
        // 헤더 타이틀과 토글
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "아이템 목록",
                // body XL/semibold
                style = MaterialTheme.walkItTypography.bodyXL,
                color = SemanticColor.textBorderPrimary,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "보유한 아이템만 보기",
                    // body S/regular
                    style = MaterialTheme.walkItTypography.bodyS
                )
                Spacer(Modifier.width(8.dp))
                CustomSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
        }

        // 카테고리 필터 (CharacterShop에서만 표시)
        if (showCategoryFilter && onCategoryFilterChange != null) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf(
                    null to "all",
                    EquipSlot.HEAD to "head",
                    EquipSlot.BODY to "body",
                    EquipSlot.FEET to "foot"
                )

                categories.forEach { (category, label) ->
                    val isSelected = selectedCategory == category

                    // Button을 사용하여 둥근 ripple 효과 적용
                    Button(
                        onClick = { onCategoryFilterChange(category) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) SemanticColor.backgroundGreenPrimary
                            else SemanticColor.backgroundWhitePrimary,
                            contentColor = if (isSelected) SemanticColor.stateGreenPrimary
                            else SemanticColor.textBorderPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = SemanticColor.stateGreenPrimary
                        ) else BorderStroke(
                            width = 1.dp,
                            color = SemanticColor.textBorderTertiary
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ItemHeaderPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        ItemHeader(checked = true, onCheckedChange = {})
    }
}