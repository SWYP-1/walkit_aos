package swyp.team.walkit.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * WalkIt 커스텀 Typography 데이터 클래스
 *
 * Material3 Typography를 확장하여 Heading XL, Body XL 등 명확한 속성을 제공합니다.
 */
data class WalkItTypography(
    val headingXL: TextStyle,
    val headingL: TextStyle,
    val headingM: TextStyle,
    val headingS: TextStyle,
    val bodyXL: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val captionM: TextStyle,
)

/**
 * WalkIt Typography 인스턴스 생성
 */
fun createWalkItTypography(): WalkItTypography {
    return WalkItTypography(
        // ========== Heading ==========
        headingXL = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.HeadingXL,
            lineHeight = TypeScale.HeadingXL * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.SemiBold,
        ),

        headingL = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.HeadingL,
            lineHeight = TypeScale.HeadingL * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.SemiBold,
        ),

        headingM = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.HeadingM,
            lineHeight = TypeScale.HeadingM * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Medium,
        ),

        headingS = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.HeadingS,
            lineHeight = TypeScale.HeadingS * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Medium,
        ),

        // ========== Body ==========
        bodyXL = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.BodyXL,
            lineHeight = TypeScale.BodyXL * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Medium,
        ),

        bodyL = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.BodyL,
            lineHeight = TypeScale.BodyL * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Normal,
        ),

        bodyM = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.BodyM,
            lineHeight = TypeScale.BodyM * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Normal,
        ),

        bodyS = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.BodyS,
            lineHeight = TypeScale.BodyS * 1.5f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Normal,
        ),

        // ========== Caption ==========
        captionM = TextStyle(
            fontFamily = Pretendard,
            fontSize = TypeScale.CaptionM,
            lineHeight = TypeScale.CaptionM * 1.3f,
            letterSpacing = TypeScale.LetterSpacing,
            fontWeight = FontWeight.Normal,
        ),
    )
}
