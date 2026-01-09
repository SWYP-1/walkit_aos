package swyp.team.walkit.ui.theme

import androidx.compose.ui.geometry.Offset
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
        darkColor: Color = Color(0xFF101010),
        startAlpha: Float = 0.1f,     // 위쪽 색상 투명도
        endAlpha: Float = 1f,       // 아래쪽 색상 투명도
        startY: Float = 0f,
        endY: Float = Float.POSITIVE_INFINITY
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                Color.Transparent, // 위쪽: 거의 투명
                darkColor.copy(alpha = endAlpha)    // 아래쪽: 50% 정도 진하기
            ),
            startY = startY,
            endY = endY
        )
    }

    /**
     * Figma 디자인 기반 그라데이션
     * 하단 20% 지점에서 어두운 색상(rgba(25,25,25,0.6))이 시작되고
     * 상단은 완전 투명한 흰색(rgba(255,255,255,0))으로 페이드되는 효과
     * 
     * @param darkColor 어두운 색상 (기본값: rgba(25,25,25,0.6))
     * @param lightColor 밝은 색상 (기본값: rgba(255,255,255,0))
     * @param fadeStartPoint 그라데이션이 시작되는 지점 (0.0~1.0, 기본값: 0.2 = 20%)
     * @param width 그라데이션 너비 (기본값: 1000f, 실제 사용 시 Modifier에서 측정된 값 사용)
     * @param height 그라데이션 높이 (기본값: 1000f, 실제 사용 시 Modifier에서 측정된 값 사용)
     */
    fun fadeToDarkFromBottom(
        darkColor: Color = Color(0xFF191919).copy(alpha = 0.6f), // rgba(25,25,25,0.6)
        lightColor: Color = Color(0xFFFFFFFF).copy(alpha = 0f), // rgba(255,255,255,0)
        fadeStartPoint: Float = 0.2f, // 20% 지점에서 시작
        width: Float = 1000f,
        height: Float = 1000f
    ): Brush {
        // linearGradient를 사용하여 정확한 색상 중지점 지정
        // 상단(0%)부터 fadeStartPoint(20%)까지는 투명한 흰색
        // fadeStartPoint(20%)부터 하단(100%)까지는 어두운 색상으로 전환
        return Brush.linearGradient(
            0.0f to lightColor,           // 상단 0%: 완전 투명한 흰색
            fadeStartPoint to lightColor, // 20% 지점까지: 투명 유지
            fadeStartPoint to darkColor,  // 20% 지점: 어두운 색상 시작
            1.0f to darkColor,           // 하단 100%: 어두운 색상 유지
            start = Offset(0f, 0f),
            end = Offset(0f, height)
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

