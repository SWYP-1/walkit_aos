package swyp.team.walkit.ui.dressroom.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.ui.components.GradeBadge
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun DressingRoomHeader(
    grade: Grade,
    level: Int? = null,
    points: Int,
    onClickQuestion: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {

        /** 🔹 중앙: 절대 중앙 고정 */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GradeBadge(grade = grade, level = level)
        }

        /** 🔹 왼쪽: 포인트 (내용 커져도 중앙 영향 없음) */
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        SemanticColor.stateYellowTertiary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SemanticColor.stateYellowPrimary
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$points",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary
            )
        }

        /** 🔹 오른쪽: 질문 아이콘 */
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onClickQuestion)
                .background(SemanticColor.iconBlack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_question),
                contentDescription = "info",
                tint = SemanticColor.iconWhite
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun DressingRoomHeaderSproutGradePreview() {
    WalkItTheme {
        DressingRoomHeader(
            grade = Grade.SPROUT,
            points = 12
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DressingRoomHeaderTreeGradePreview() {
    WalkItTheme {
        DressingRoomHeader(
            grade = Grade.TREE,
            points = 1234
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DressingRoomHeaderLongNickNamePreview() {
    WalkItTheme {
        DressingRoomHeader(
            grade = Grade.SPROUT,
            points = 1245
        )
    }
}
