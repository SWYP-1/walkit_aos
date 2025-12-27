package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.DateUtils
import team.swyp.sdu.utils.Season

@Composable
fun CharacterAndBackground(
    modifier: Modifier = Modifier,
    character: Character,
    points: Int,
    onBackClick: () -> Unit = {},
    onQuestionClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    // 오늘 날짜의 계절 확인
    val currentSeason = DateUtils.getCurrentSeason()
    val backgroundRes =
        when (currentSeason) {
            Season.SPRING -> R.drawable.bg_spring_cropped
            Season.SUMMER -> R.drawable.bg_summer_cropped
            Season.AUTUMN -> R.drawable.bg_autom_cropped
            Season.WINTER -> R.drawable.bg_winter_cropped
        }

    Box(modifier = modifier.fillMaxWidth()) {
        // 1️⃣ 배경
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = "season background",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(25f / 32f),
            contentScale = ContentScale.Crop,
        )

        // 2️⃣ 헤더 (상단)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp) // System Bar 표준 높이 사용
        ) {
            DressingRoomHeader(
                grade = character.grade,
                nickName = character.nickName,
                onBack = onBackClick,
                onClickQuestion = onQuestionClick
            )
        }

        // 3️⃣ start / bottom 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(SemanticColor.backgroundDarkPrimary)
                .clickable(onClick = onRefreshClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_refresh),
                contentDescription = "refresh",
                tint = SemanticColor.iconWhite
            )
        }

        // 4️⃣ end / bottom 포인트 박스
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .background(
                    SemanticColor.stateYellowTertiary,
                    shape = RoundedCornerShape(9.6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "보유 포인트",
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.stateYellowPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${points}P",
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.stateYellowPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CharacterAndBackgroundPreview() {
    val dummyCharacter = Character(
        nickName = "승우",
        grade = Grade.TREE,
    )
    WalkItTheme {
        CharacterAndBackground(
            character = dummyCharacter,
            points = 500,
            onBackClick = { /* 프리뷰용 클릭 */ },
            onQuestionClick = { /* 프리뷰용 클릭 */ },
            onRefreshClick = { /* 프리뷰용 클릭 */ }
        )
    }
}
