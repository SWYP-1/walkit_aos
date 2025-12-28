package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * 산책 진행 단계 표시기
 * 
 * Figma 디자인 기반: 3개의 진행 바로 구성
 * - 각 바: 107px 너비, 5px 높이, 10px 간격, 50px 모서리 반경
 * - Active: 녹색 (#52ce4b)
 * - Default: 회색 (#f3f3f5)
 * 
 * @param currentStep 현재 단계 (1, 2, 3)
 * @param modifier Modifier
 */
@Composable
fun WalkingProgressBar(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val stepNumber = index + 1
            val isActive = stepNumber <= currentStep
            
            // 진행 바
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(
                        color = if (isActive) {
                            Color(0xFF52CE4B) // button/primary-default
                        } else {
                            Color(0xFFF3F3F5) // text-border/secondary-inverse
                        },
                        shape = RoundedCornerShape(50.dp), // 50px 모서리 반경
                    ),
            )
            
            // 마지막 바가 아니면 간격 추가
            if (stepNumber < 3) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WalkingProgressBarPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
        ) {
            WalkingProgressBar(currentStep = 1)
            WalkingProgressBar(currentStep = 2)
            WalkingProgressBar(currentStep = 3)
        }
    }
}






