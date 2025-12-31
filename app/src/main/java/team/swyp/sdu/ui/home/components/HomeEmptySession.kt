package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun HomeEmptySession(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(Grey3, shape = RoundedCornerShape(16.dp))
            .padding(vertical = 36.dp, horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_session),
            contentDescription = null,
            modifier = modifier
        )
        Text(
            text = "아직 산책 기록이 없어요",
            // body XL/semibold
            style = MaterialTheme.walkItTypography.bodyXL.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = SemanticColor.textBorderPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "워킷과 함께 산책하고 나만의 산책 기록을 남겨보세요.",

            // body S/medium
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.textBorderSecondary
        )
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .background(
                    color = SemanticColor.textBorderPrimary,
                    shape = RoundedCornerShape(size = 8.dp)
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
                .clickableNoRipple(onClick)
        ) {
            Text(
                text = "산책하러 가기",
                // body S/semibold
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimaryInverse
            )
        }
    }
}

@Preview
@Composable
fun HomeEmptySessionPrivew(modifier: Modifier = Modifier) {
    WalkItTheme {
        HomeEmptySession(Modifier, {})
    }
}