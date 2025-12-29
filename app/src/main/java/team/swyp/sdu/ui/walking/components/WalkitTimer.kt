package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun WalkitTimer(duration: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "산책 시간",

            // body M/medium
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.iconGrey,
        )

        Text(
            text = formatDurationWithMillis(duration),
            style = MaterialTheme.walkItTypography.headingXL.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                lineHeight = 67.6.sp,
            ),
            color = SemanticColor.logoGreen
        )
    }
}