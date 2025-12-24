package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * Grade 배지 컴포넌트
 *
 * Grade enum에 따라 색상과 텍스트가 자동으로 변경됩니다.
 * 레벨은 Grade enum의 level 속성에서 자동으로 가져옵니다.
 * - SEED: "Lv.1 새싹" (초록색)
 * - SPROUT: "Lv.2 묘목" (청록색)
 * - TREE: "Lv.3 나무" (보라색)
 */
@Composable
fun GradeBadge(
    grade: Grade,
    modifier: Modifier = Modifier,
) {
    val (text, backgroundColor, textColor) = when (grade) {
        Grade.SEED -> Triple(
            "Lv.${grade.level} 새싹",
            SemanticColor.stateGreenTertiary,
            SemanticColor.stateGreenPrimary,
        )
        Grade.SPROUT -> Triple(
            "Lv.${grade.level} 묘목",
            SemanticColor.stateAquaBlueTertiary,
            SemanticColor.stateAquaBluePrimary,
        )
        Grade.TREE -> Triple(
            "Lv.${grade.level} 나무",
            SemanticColor.statePurpleTertiary,
            SemanticColor.statePurplePrimary,
        )
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = textColor,
        )
    }
}

@Preview(showBackground = true, name = "SEED")
@Composable
private fun GradeBadgeSeedPreview() {
    WalkItTheme {
        GradeBadge(grade = Grade.SEED)
    }
}

@Preview(showBackground = true, name = "SPROUT")
@Composable
private fun GradeBadgeSproutPreview() {
    WalkItTheme {
        GradeBadge(grade = Grade.SPROUT)
    }
}

@Preview(showBackground = true, name = "TREE")
@Composable
private fun GradeBadgeTreePreview() {
    WalkItTheme {
        GradeBadge(grade = Grade.TREE)
    }
}

