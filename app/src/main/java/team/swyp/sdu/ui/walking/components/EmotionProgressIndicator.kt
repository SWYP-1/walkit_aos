package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.WalkItTheme

/**
 * 감정 기록 단계 진행률 표시기
 * @param currentStep 현재 단계 (1 또는 2)
 * @param totalSteps 전체 단계 수 (기본값: 3)
 */
@Composable
fun EmotionProgressIndicator(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            Box(
                modifier = Modifier
                    .width(if (stepNumber == currentStep) 40.dp else 20.dp)
                    .height(4.dp)
                    .background(
                        color = if (stepNumber <= currentStep) {
                            Color(0xFF2E2E2E)
                        } else {
                            Color(0xFFE5E5E5)
                        },
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
            if (stepNumber < totalSteps) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Preview
@Composable
fun EmotionProgressIndicatorPreview() {
    WalkItTheme {
        EmotionProgressIndicator(currentStep = 2, totalSteps = 5)
    }
}
