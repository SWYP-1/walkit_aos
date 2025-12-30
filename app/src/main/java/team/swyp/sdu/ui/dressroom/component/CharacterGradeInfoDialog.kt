package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun CharacterGradeInfoDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val maxHeight = 140.dp
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = SemanticColor.backgroundWhitePrimary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(vertical = 28.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "캐릭터 레벨",

                    // body XL/semibold
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimary
                )

                IconButton(
                    modifier = modifier.size(24.dp),
                    onClick = onDismiss
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_clear),
                        contentDescription = "claer"
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Column {
                CharacterGradeDescription(
                    modifier = Modifier.height(maxHeight),
                    grade = Grade.SEED,
                    startIndex = 1,
                    desList = listOf("누적 주간 목표 1주 달성", "누적 주간 목표 4주 달성", "누적 주간 목표 6주 달성")
                )
                Spacer(Modifier.height(16.dp))
                CharacterGradeDescription(
                    modifier = Modifier.height(maxHeight),
                    grade = Grade.SPROUT,
                    startIndex = 4,
                    desList = listOf(
                        "누적 주간 목표 8주 달성",
                        "누적 주간 목표 10주 달성",
                        "누적 주간 목표 2주 달성",
                        "누적 주간 목표 4주 달성"
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "새싹 Lv.06부터 레벨업 달성 시 이전 기록이 초기화 됩니다.",

                    // caption M/regular
                    style = MaterialTheme.walkItTypography.captionM,
                    color = SemanticColor.textBorderTertiary
                )
                Spacer(Modifier.height(16.dp))
                CharacterGradeDescription(
                    modifier = Modifier.height(maxHeight),
                    grade = Grade.TREE, startIndex = 5, desList = listOf(
                        "누적 주간 목표 6주 달성",
                        "누적 주간 목표 8주 달성",
                        "누적 주간 목표 10주 달성",
                    )
                )
            }
        }
    }
}

@Composable
private fun CharacterGradeDescription(
    modifier: Modifier = Modifier,
    grade: Grade,
    desList: List<String>,
    startIndex: Int
) {
    val painterResource = when (grade) {
        Grade.SEED -> painterResource(R.drawable.ic_seed)
        Grade.SPROUT -> painterResource(R.drawable.ic_sprout)
        Grade.TREE -> painterResource(R.drawable.ic_tree)
    }

    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight()
        ) {
            Image(
                painter = painterResource,
                modifier = Modifier.size(56.dp),
                contentDescription = "character image"
            )
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .background(
                        color = SemanticColor.stateBlueTertiary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = grade.name,
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.stateAquaBluePrimary
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // LazyColumn 대신 Column + height 분배
        Column(
            modifier = Modifier
                .background(SemanticColor.backgroundWhiteSecondary)
                .padding(vertical = 16.dp, horizontal = 12.dp)
                .weight(1f)
        ) {
            desList.forEachIndexed { index, des ->
                // 높이 균등 분배
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CharacterDesListItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    )
                }

                // 마지막 아이템 뒤에는 Divider 없음
                if (index < desList.lastIndex) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Gray)
                    )
                }
            }
        }
    }
}


@Composable
fun CharacterDesListItem(modifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Lv.01",

            // body S/medium
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.textBorderPrimary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "누적 주간 목표 1주 달성",

            // caption M/regular
            style = MaterialTheme.walkItTypography.captionM,
            color = SemanticColor.textBorderPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CharacterGradeInfoDialogPreview() {
    WalkItTheme {
        CharacterGradeInfoDialog()
    }
}