package team.swyp.sdu.ui.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.kakaoYellow
import team.swyp.sdu.ui.theme.naverGreen
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun LoginButton(
    backgroundColor: Color,
    provider: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val iconDrawable = when (provider) {
        "카카오" -> R.drawable.ic_login_kakao
        "네이버" -> R.drawable.ic_login_naver
        else -> R.drawable.ic_login_kakao
    }
    val fontColor = when (provider) {
        "카카오" -> SemanticColor.iconBlack
        "네이버" -> SemanticColor.iconWhite
        else -> SemanticColor.iconWhite
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick // Surface의 onClick은 ripple을 자동 적용
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(iconDrawable),
                contentDescription = "login provider",
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$provider 로그인",
                style = MaterialTheme.walkItTypography.bodyXL,
                color = fontColor
            )
            Text("")
        }
    }
}

@Preview
@Composable
fun LoginButtonNaverPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        LoginButton(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = naverGreen,
            provider = "네이버"
        ) {

        }
    }
}

@Preview
@Composable
fun LoginButtonKakaoPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        LoginButton(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = kakaoYellow,
            provider = "카카오"
        ) {

        }
    }
}












