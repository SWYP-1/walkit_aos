package team.swyp.sdu.ui.mission.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme

/**
 * 카테고리 칩 컴포넌트
 *
 * 필터링을 위한 카테고리 선택 칩
 */
@Composable
fun CategoryChip(
    category: MissionCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        SemanticColor.backgroundGreenPrimary// 선택된 상태: primary 색상 배경
    } else {
        SemanticColor.backgroundWhitePrimary // 선택되지 않은 상태: 부드러운 배경
    }

    val borderColor = if (isSelected) {
        SemanticColor.stateGreenPrimary // 선택된 상태: primary 테두리
    } else {
        SemanticColor.textBorderSecondary // 선택되지 않은 상태: 테두리 없음
    }

    val textColor = if (isSelected) {
        SemanticColor.stateGreenPrimary // 선택된 상태: 흰색 텍스트
    } else {
        SemanticColor.textBorderPrimary // 선택되지 않은 상태: 기본 텍스트
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

// Preview functions
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "CategoryChip - Not Selected")
@Composable
private fun CategoryChipNotSelectedPreview() {
    WalkItTheme {
        CategoryChip(
            category = team.swyp.sdu.domain.model.MissionCategory.CHALLENGE_STEPS,
            isSelected = false,
            onClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "CategoryChip - Selected")
@Composable
private fun CategoryChipSelectedPreview() {
    WalkItTheme {
        CategoryChip(
            category = team.swyp.sdu.domain.model.MissionCategory.CHALLENGE_ATTENDANCE,
            isSelected = true,
            onClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "CategoryChip - All States")
@Composable
private fun CategoryChipAllStatesPreview() {
    WalkItTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = androidx.compose.ui.Modifier.padding(16.dp)
        ) {
            CategoryChip(
                category = team.swyp.sdu.domain.model.MissionCategory.CHALLENGE_STEPS,
                isSelected = true,
                onClick = {}
            )
            CategoryChip(
                category = team.swyp.sdu.domain.model.MissionCategory.CHALLENGE_ATTENDANCE,
                isSelected = false,
                onClick = {}
            )
        }
    }
}
