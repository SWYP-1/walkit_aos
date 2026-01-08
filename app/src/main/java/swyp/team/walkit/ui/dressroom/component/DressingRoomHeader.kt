package swyp.team.walkit.ui.dressroom.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
    grade: swyp.team.walkit.domain.model.Grade,
    level: Int? = null,
    nickName: String,
    onBack: () -> Unit = {},
    onClickQuestion: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 뒤로가기 버튼 클릭 영역 확대
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp) // 최소 터치 영역 확보
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_backward),
                contentDescription = "arrow back",
                modifier = Modifier.size(24.dp) // 아이콘 크기
            )
        }

        // 닉네임 + 등급
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f) // 남는 공간 채우기
        ) {

            GradeBadge(grade = grade, level = level)
            Spacer(Modifier.width(8.dp))
            Text(
                text = nickName,
                style = MaterialTheme.walkItTypography.headingS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 오른쪽 질문 아이콘

        IconButton(
            onClick = onClickQuestion,
            modifier = Modifier.size(48.dp) // 최소 터치 영역 확보
        ){
            Box(
                modifier = Modifier
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
}

@Preview(showBackground = true)
@Composable
private fun DressingRoomHeaderSproutGradePreview() {
    WalkItTheme {
        DressingRoomHeader(
            grade = Grade.SPROUT,
            nickName = "성장중인사용자"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DressingRoomHeaderTreeGradePreview() {
    WalkItTheme {
        DressingRoomHeader(
            grade = Grade.TREE,
            nickName = "완성된나무"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DressingRoomHeaderLongNickNamePreview() {
    WalkItTheme {
        DressingRoomHeader(
            grade = Grade.SPROUT,
            nickName = "매우긴닉네임을가진사용자가 이름이 더길어진다."
        )
    }
}
