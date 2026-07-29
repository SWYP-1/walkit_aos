package swyp.team.walkit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

//TODO : DarkMode 구현
private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
        background = Color.White,
        surface = Color.White,
    )

/**
 * WalkIt Typography를 제공하는 CompositionLocal
 */
val LocalWalkItTypography = compositionLocalOf<WalkItTypography> {
    error("WalkItTypography not provided")
}

/**
 * MaterialTheme 확장: WalkIt Typography 접근
 *
 * 사용 예시:
 * ```kotlin
 * Text(
 *     text = "제목",
 *     style = MaterialTheme.walkItTypography.headingXL
 * )
 * ```
 */
val MaterialTheme.walkItTypography: WalkItTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalWalkItTypography.current

/**
 * WalkIt Typography를 Material3 Typography로 변환
 */
private fun createMaterial3Typography(
    walkIt: WalkItTypography,
): Typography {
    return Typography(
        displayLarge = walkIt.headingXL,
        displayMedium = walkIt.headingL,
        displaySmall = walkIt.headingM,
        headlineSmall = walkIt.headingS,
        bodyLarge = walkIt.bodyXL,
        bodyMedium = walkIt.bodyL,
        bodySmall = walkIt.bodyM,
        labelSmall = walkIt.captionM,
    )
}

@Composable
fun WalkItTheme(
    content: @Composable () -> Unit,
) {
    val walkItTypography = createWalkItTypography()

    CompositionLocalProvider(
        LocalWalkItTypography provides walkItTypography,
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = createMaterial3Typography(walkItTypography),
            content = content,
        )
    }
}