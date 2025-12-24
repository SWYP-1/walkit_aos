package team.swyp.sdu.ui.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun OnBoardingStepTag(modifier: Modifier = Modifier,text : String) {
    Box(
        modifier = Modifier
            .background(
                color = SemanticColor.backgroundDarkPrimary,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = SemanticColor.textBorderPrimaryInverse,
        )
    }
}