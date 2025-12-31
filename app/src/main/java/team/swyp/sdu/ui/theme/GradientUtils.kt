package team.swyp.sdu.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 그라데이션 관련 유틸리티 함수들
 */
object GradientUtils {

    /**
     * 투명에서 어두운 색상으로 수직 그라데이션
     * 주로 카드 하단 그라데이션 효과에 사용
     */
    fun fadeToDark(
        darkColor: Color = Color(0xFF191919),
        startY: Float = 0f,
        endY: Float = Float.POSITIVE_INFINITY
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,        // 위 투명
                darkColor
            ),
            startY = startY,
            endY = endY
        )
    }

    /**
     * 투명에서 회색으로 수직 그라데이션
     * FriendRecord 등에서 사용
     */
    fun fadeToGray(
        grayColor: Color = Color(0xFF444444),
        startY: Float = 0f,
        endY: Float = Float.POSITIVE_INFINITY
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,        // 위 투명
                grayColor
            ),
            startY = startY,
            endY = endY
        )
    }

    /**
     * 중앙 fade 효과 그라데이션
     * WheelPicker 등에서 중앙 강조 효과에 사용
     */
    fun centerFade(
        surfaceColor: Color,
        startY: Float = 0f,
        endY: Float
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                surfaceColor,
                surfaceColor.copy(alpha = 0.35f),
                surfaceColor.copy(alpha = 0f),
                surfaceColor.copy(alpha = 0.35f),
                surfaceColor,
            ),
            startY = startY,
            endY = endY
        )
    }
}
