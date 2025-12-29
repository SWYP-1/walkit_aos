package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun WalkingActionButton(
    iconRes: Int,
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = SemanticColor.stateGreenSecondary,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 12.dp, // ⭐️ 이게 핵심
        modifier = modifier.size(92.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 🔒 아이콘 영역 고정
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = SemanticColor.textBorderGreenSecondary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = text,
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}



@Preview
@Composable
fun WalkingActionButtonPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        WalkingActionButton(
            iconRes = R.drawable.ic_action_pause,
            text = "일시정지",
            backgroundColor = SemanticColor.backgroundWhitePrimary,
            modifier = modifier,
            textColor = SemanticColor.textBorderGreenSecondary,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun WalkingActionButtonFinishPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        WalkingActionButton(
            iconRes = R.drawable.ic_action_finish_walk,
            text = "산책 끝내기",
            backgroundColor = Color(0xFFD8FFD6),
            modifier = modifier,
            textColor = SemanticColor.textBorderGreenSecondary,
            onClick = {}
        )
    }
}