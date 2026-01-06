package swyp.team.walkit.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/**
 * 프리미엄 쉬머 효과 스타일
 */
enum class ShimmerStyle {
    /** 배달의민족 스타일 - 부드러운 흰색 그라데이션 */
    BAEMIN,
    /** 당근마켓 스타일 - 오렌지 계열 그라데이션 */
    KARROT,
    /** 토스 스타일 - 블루 계열 그라데이션 */
    TOSS,
    /** 기본 스타일 - 그레이 계열 */
    DEFAULT,
    /** 다크모드 스타일 */
    DARK
}

/**
 * 고급스러운 쉬머 효과
 *
 * @param style 쉬머 스타일
 * @param durationMillis 애니메이션 지속 시간
 * @param gradientWidth 그라데이션 너비 비율 (0.3 = 30%)
 * @param angle 쉬머 각도 (도 단위, 0 = 가로, 90 = 세로, 45 = 대각선)
 */
fun Modifier.shimmer(
    style: ShimmerStyle = ShimmerStyle.DEFAULT,
    durationMillis: Int = 1300,
    gradientWidth: Float = 0.4f,
    angle: Float = 20f
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val colors = when (style) {
        ShimmerStyle.BAEMIN -> listOf(
            Color(0xFFE8E8E8),
            Color(0xFFF8F8F8),
            Color(0xFFFFFFFF),
            Color(0xFFF8F8F8),
            Color(0xFFE8E8E8)
        )
        ShimmerStyle.KARROT -> listOf(
            Color(0xFFFFE8D6),
            Color(0xFFFFF3E8),
            Color(0xFFFFFAF5),
            Color(0xFFFFF3E8),
            Color(0xFFFFE8D6)
        )
        ShimmerStyle.TOSS -> listOf(
            Color(0xFFE3F2FD),
            Color(0xFFEFF6FF),
            Color(0xFFF8FBFF),
            Color(0xFFEFF6FF),
            Color(0xFFE3F2FD)
        )
        ShimmerStyle.DARK -> listOf(
            Color(0xFF2C2C2C),
            Color(0xFF3A3A3A),
            Color(0xFF484848),
            Color(0xFF3A3A3A),
            Color(0xFF2C2C2C)
        )
        ShimmerStyle.DEFAULT -> listOf(
            Color(0xFFE0E0E0),
            Color(0xFFEFEFEF),
            Color(0xFFF5F5F5),
            Color(0xFFEFEFEF),
            Color(0xFFE0E0E0)
        )
    }

    this
        .onGloballyPositioned { coordinates ->
            size = coordinates.size
        }
        .background(
            brush = if (size.width > 0 && size.height > 0) {
                createAngledShimmerBrush(
                    colors = colors,
                    size = size,
                    progress = progress,
                    gradientWidth = gradientWidth,
                    angle = angle
                )
            } else {
                Brush.linearGradient(colors = listOf(colors[0], colors[0]))
            }
        )
}

/**
 * 각도가 적용된 쉬머 브러시 생성
 */
private fun createAngledShimmerBrush(
    colors: List<Color>,
    size: IntSize,
    progress: Float,
    gradientWidth: Float,
    angle: Float
): Brush {
    val angleRad = Math.toRadians(angle.toDouble())
    val cos = kotlin.math.cos(angleRad).toFloat()
    val sin = kotlin.math.sin(angleRad).toFloat()

    // 대각선 길이 계산
    val diagonal = kotlin.math.sqrt(
        (size.width * size.width + size.height * size.height).toDouble()
    ).toFloat()

    // 진행 거리 계산 (대각선 + 여유 공간)
    val totalDistance = diagonal * (1f + gradientWidth)
    val currentDistance = -diagonal * gradientWidth + progress * totalDistance

    // 시작점과 끝점 계산
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    val gradientLength = diagonal * gradientWidth

    val startX = centerX + (currentDistance - gradientLength / 2) * cos
    val startY = centerY + (currentDistance - gradientLength / 2) * sin
    val endX = centerX + (currentDistance + gradientLength / 2) * cos
    val endY = centerY + (currentDistance + gradientLength / 2) * sin

    return Brush.linearGradient(
        colors = colors,
        start = Offset(startX, startY),
        end = Offset(endX, endY)
    )
}

/**
 * 부드러운 페이드 인/아웃 쉬머 (배민 스타일)
 */
fun Modifier.shimmer(): Modifier = shimmer(
    style = ShimmerStyle.BAEMIN,
    durationMillis = 1500,
    gradientWidth = 0.5f,
    angle = 15f
)

/**
 * 빠르고 역동적인 쉬머 (토스 스타일)
 */
fun Modifier.shimmerToss(): Modifier = shimmer(
    style = ShimmerStyle.TOSS,
    durationMillis = 1000,
    gradientWidth = 0.3f,
    angle = 25f
)

/**
 * 따뜻한 느낌의 쉬머 (당근 스타일)
 */
fun Modifier.shimmerKarrot(): Modifier = shimmer(
    style = ShimmerStyle.KARROT,
    durationMillis = 1400,
    gradientWidth = 0.4f,
    angle = 20f
)

/**
 * 다크모드용 쉬머
 */
fun Modifier.shimmerDark(): Modifier = shimmer(
    style = ShimmerStyle.DARK,
    durationMillis = 1300,
    gradientWidth = 0.4f,
    angle = 20f
)