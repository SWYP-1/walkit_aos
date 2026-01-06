package swyp.team.walkit.ui.mypage.component

import android.R
import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun ServiceInfoFooter(
    modifier: Modifier = Modifier,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onMarketingClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onCsChannelClick: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(SemanticColor.backgroundWhiteSecondary).padding(vertical = 20.dp, horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "서비스 이용 약관",

                // caption M/medium
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary,
                modifier = Modifier.clickable(onClick = onTermsClick)
            )

            VerticalDivider(
                Modifier
                    .width(1.dp)
                    .height(16.dp)
            )

            Text(
                text = "개인정보처리 방침",

                // caption M/medium
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary,
                modifier = Modifier.clickable(onClick = onPrivacyClick)
            )
            VerticalDivider(
                Modifier
                    .width(1.dp)
                    .height(16.dp)
            )

            Text(
                text = "마케팅 수신 동의",

                // caption M/medium
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary,
                modifier = Modifier.clickable(onClick = onMarketingClick),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "문의하기",

                // caption M/medium
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary,
                modifier = Modifier.clickable(onClick = onContactClick)
            )
        }
    }
}

@Preview
@Composable
fun ServiceInfoFooterPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        ServiceInfoFooter(
            onTermsClick = {},
            onPrivacyClick = {},
            onMarketingClick = {},
            onContactClick = {},
            onCsChannelClick = {},
        )
    }
}