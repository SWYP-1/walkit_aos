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
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * Grade 배지 컴포넌트
 *
 * Grade enum에 따라 색상과 텍스트가 자동으로 변경됩니다.
 * 레벨은 Grade enum의 level 속성에서 자동으로 가져옵니다.
 * - SEED: "Lv.1 씨앗" (초록색)
 * - SPROUT: "Lv.2 새싹" (청록색)
 * - TREE: "Lv.3 나무" (보라색)
 */
@Composable
fun GradeBadge(
    grade: Grade,
    level: Int? = null, // character의 실제 level을 사용할 수 있도록 추가
    modifier: Modifier = Modifier,
) {
    // level 파라미터가 있으면 사용, 없으면 grade의 기본 level 사용
    val displayLevel = level ?: grade.level

    val (text, backgroundColor, textColor) = when (grade) {
        Grade.SEED -> Triple(
            "Lv.$displayLevel 씨앗",
            SemanticColor.stateAquaBlueTertiary,
            SemanticColor.stateAquaBluePrimary,
        )
        Grade.SPROUT -> Triple(
            "Lv.$displayLevel 새싹",
            SemanticColor.stateAquaBlueTertiary,
            SemanticColor.stateAquaBluePrimary,
        )
        Grade.TREE -> Triple(
            "Lv.$displayLevel 나무",
            SemanticColor.stateAquaBlueTertiary,
            SemanticColor.stateAquaBluePrimary,
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
                vertical = 8.dp,
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

