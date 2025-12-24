package team.swyp.sdu.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme

/**
 * 커스텀 로딩 프로그레스 인디케이터
 *
 * Figma 디자인에 맞춘 8개의 막대가 원형으로 배치되어 회전하는 로딩 인디케이터입니다.
 *
 * @param size 인디케이터 크기 (small: 36dp, medium: 40dp)
 * @param color 막대 색상 (기본값: SemanticColor.textBorderSecondary)
 * @param modifier Modifier
 */
@Composable
fun CustomProgressIndicator(
    size: ProgressIndicatorSize = ProgressIndicatorSize.Small,
    color: Color = SemanticColor.textBorderSecondary,
    modifier: Modifier = Modifier,
) {
    val indicatorSize = when (size) {
        ProgressIndicatorSize.Small -> 36.dp
        ProgressIndicatorSize.Medium -> 40.dp
    }

    val barWidth = when (size) {
        ProgressIndicatorSize.Small -> 4.8.dp
        ProgressIndicatorSize.Medium -> 5.333.dp
    }

    val barHeight = when (size) {
        ProgressIndicatorSize.Small -> 12.dp
        ProgressIndicatorSize.Medium -> 13.333.dp
    }

    // 무한 회전 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        modifier = modifier
            .size(indicatorSize)
            .rotate(rotationAngle),
        contentAlignment = Alignment.Center,
    ) {
        // 8개의 막대를 원형으로 배치
        // 각 막대는 다른 각도와 opacity를 가짐
        val bars = listOf(
            BarConfig(angle = 0f, opacity = 1.0f),      // 0° - opacity 1.0
            BarConfig(angle = 45f, opacity = 0.15f),    // 45° - opacity 0.15
            BarConfig(angle = 90f, opacity = 0.27f),     // 90° - opacity 0.27
            BarConfig(angle = 135f, opacity = 0.39f),    // 135° - opacity 0.39
            BarConfig(angle = 180f, opacity = 0.51f),    // 180° - opacity 0.51
            BarConfig(angle = 225f, opacity = 0.63f),    // 225° - opacity 0.63
            BarConfig(angle = 270f, opacity = 0.75f),    // 270° - opacity 0.75
            BarConfig(angle = 325f, opacity = 0.87f),    // 325° - opacity 0.87
        )

        bars.forEach { barConfig ->
            ProgressBar(
                angle = barConfig.angle,
                opacity = barConfig.opacity,
                barWidth = barWidth,
                barHeight = barHeight,
                containerSize = indicatorSize,
                color = color,
            )
        }
    }
}

/**
 * 개별 프로그레스 바 컴포넌트
 */
@Composable
private fun ProgressBar(
    angle: Float,
    opacity: Float,
    barWidth: Dp,
    barHeight: Dp,
    containerSize: Dp,
    color: Color,
) {
    // 막대의 위치 계산 (원형 배치)
    // 각 막대는 중심에서 반지름만큼 떨어진 위치에 배치됨
    val radius = containerSize / 2 - barHeight / 2
    val radians = Math.toRadians(angle.toDouble())
    
    // 중심을 기준으로 한 위치 계산
    val centerX = containerSize.value / 2
    val centerY = containerSize.value / 2
    val xOffset = centerX + (radius.value * Math.cos(radians)).toFloat() - barWidth.value / 2
    val yOffset = centerY + (radius.value * Math.sin(radians)).toFloat() - barHeight.value / 2

    Box(
        modifier = Modifier
            .offset(x = xOffset.dp, y = yOffset.dp)
            .size(width = barWidth, height = barHeight)
            .rotate(angle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = color.copy(alpha = opacity),
                    shape = RoundedCornerShape(4.dp),
                ),
        )
    }
}

/**
 * 프로그레스 인디케이터 크기
 */
enum class ProgressIndicatorSize {
    Small,
    Medium,
}

/**
 * 막대 설정 데이터 클래스
 */
private data class BarConfig(
    val angle: Float,
    val opacity: Float,
)

@Preview(showBackground = true, name = "Small Size")
@Composable
private fun CustomProgressIndicatorSmallPreview() {
    WalkItTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(SemanticColor.backgroundWhiteSecondary),
            contentAlignment = Alignment.Center,
        ) {
            CustomProgressIndicator(
                size = ProgressIndicatorSize.Small,
            )
        }
    }
}

@Preview(showBackground = true, name = "Medium Size")
@Composable
private fun CustomProgressIndicatorMediumPreview() {
    WalkItTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(SemanticColor.backgroundWhiteSecondary),
            contentAlignment = Alignment.Center,
        ) {
            CustomProgressIndicator(
                size = ProgressIndicatorSize.Medium,
            )
        }
    }
}

