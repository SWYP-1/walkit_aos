package swyp.team.walkit.ui.walking.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.Grey7
import swyp.team.walkit.ui.theme.SemanticColor

/**
 * 저장 중 로딩 오버레이
 * 반투명 배경 + 중앙 커스텀 프로그래스 바
 */
@Composable
fun WalkingResultLoadingOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        CustomProgressIndicator(
            size = ProgressIndicatorSize.Medium,
        )
    }
}

/**
 * 프로그레스 인디케이터 크기
 */
enum class ProgressIndicatorSize(val size: Float) {
    Small(36f),
    Medium(40f),
}

/**
 * 커스텀 프로그레스 인디케이터
 * Figma 디자인 기반: 8개의 사각형이 원형으로 배치되고 회전하는 애니메이션
 */
@Composable
fun CustomProgressIndicator(
    size: ProgressIndicatorSize = ProgressIndicatorSize.Small,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress_indicator")
    
    // 회전 애니메이션 (0도에서 360도까지 무한 반복)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1200,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        modifier = modifier
            .size(size.size.dp)
            .rotate(rotation),
        contentAlignment = Alignment.Center,
    ) {
        // 8개의 사각형을 원형으로 배치
        // 각 사각형의 위치와 opacity는 Figma 디자인에 맞춤
        ProgressSegment(
            angle = 0f,
            opacity = 1.0f,
            size = size.size,
        )
        ProgressSegment(
            angle = 45f,
            opacity = 0.15f,
            size = size.size,
        )
        ProgressSegment(
            angle = 90f,
            opacity = 0.27f,
            size = size.size,
        )
        ProgressSegment(
            angle = 135f,
            opacity = 0.39f,
            size = size.size,
        )
        ProgressSegment(
            angle = 180f,
            opacity = 0.51f,
            size = size.size,
        )
        ProgressSegment(
            angle = 225f,
            opacity = 0.63f,
            size = size.size,
        )
        ProgressSegment(
            angle = 270f,
            opacity = 0.75f,
            size = size.size,
        )
        ProgressSegment(
            angle = 325f,
            opacity = 0.87f,
            size = size.size,
        )
    }
}

/**
 * 프로그레스 인디케이터의 개별 세그먼트
 */
@Composable
private fun ProgressSegment(
    angle: Float,
    opacity: Float,
    size: Float,
) {
    val segmentWidth = if (size == 36f) 4.8f else 5.333f
    val segmentHeight = if (size == 36f) 12f else 13.333f
    
    // 원의 중심에서 거리 계산 (반지름)
    // Figma 디자인 기준: 세그먼트가 원의 둘레에 배치됨
    val radius = size * 0.3f
    
    // 각도를 라디안으로 변환
    val angleRad = Math.toRadians(angle.toDouble())
    
    // x, y 좌표 계산 (세그먼트의 중심점)
    val centerX = (radius * kotlin.math.cos(angleRad)).toFloat()
    val centerY = (radius * kotlin.math.sin(angleRad)).toFloat()
    
    // 세그먼트 자체의 회전 각도 (원의 접선 방향으로 배치)
    val segmentRotation = angle + 90f
    
    Box(
        modifier = Modifier
            .offset(x = centerX.dp, y = centerY.dp)
            .rotate(segmentRotation)
            .size(width = segmentWidth.dp, height = segmentHeight.dp)
            .background(
                color = Grey7.copy(alpha = opacity),
                shape = RoundedCornerShape(4.dp),
            ),
    )
}



