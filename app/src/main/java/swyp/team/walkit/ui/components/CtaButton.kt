package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import swyp.team.walkit.ui.theme.Green4
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.White
import swyp.team.walkit.ui.theme.walkItTypography


enum class ButtonSize(val height: Dp) {
    MEDIUM(46.dp),  // 일반 버튼 (46px)
    SMALL(40.dp)    // 작은 버튼 (40px)
}

enum class CtaButtonSize(val height: Dp) {
    MEDIUM(52.dp),  // CTA 버튼 (52px)
    SMALL(46.dp)    // CTA 작은 버튼 (46px) - 기본값
}

enum class CtaButtonVariant {
    PRIMARY,    // 초록 배경 + 흰 글자
    SECONDARY   // 흰 배경 + 초록 글자 + 초록 테두리
}

data class CtaButtonColors(
    val container: Color,
    val content: Color,
    val border: Color? = null,
)

@Composable
private fun ctaButtonColors(
    variant: CtaButtonVariant,
    enabled: Boolean,
): CtaButtonColors {
    return when {
        !enabled -> CtaButtonColors(
            container = SemanticColor.buttonPrimaryDisabled,
            content = SemanticColor.iconGrey,
        )

        variant == CtaButtonVariant.PRIMARY -> CtaButtonColors(
            container = SemanticColor.buttonPrimaryDefault,
            content = SemanticColor.backgroundWhitePrimary,
        )

        else -> CtaButtonColors(
            container = White,
            content = SemanticColor.textBorderGreenPrimary,
            border = SemanticColor.textBorderGreenPrimary,
        )
    }
}


@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: CtaButtonVariant = CtaButtonVariant.PRIMARY,
    size: CtaButtonSize = CtaButtonSize.SMALL,
    iconResId: Int? = null,
    iconTint: Color? = null,
) {
    val colors = ctaButtonColors(variant, enabled)
    val finalIconTint = iconTint ?: colors.content

    Box(
        modifier = modifier
            .height(size.height)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.container)
            .then(
                colors.border?.let {
                    Modifier.border(
                        width = 1.dp,
                        color = it,
                        shape = RoundedCornerShape(8.dp)
                    )
                } ?: Modifier
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {

            Text(
                text = text,
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.content,
                maxLines = 1
            )

            iconResId?.let {
                Spacer(Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = it),
                    contentDescription = null,
                    tint = finalIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
/**
 * CtaButton Preview
 */
@Preview(name = "Primary")
@Composable
fun CtaPrimaryPreview() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            onClick = {},
            variant = CtaButtonVariant.PRIMARY
        )
    }
}

@Preview(name = "Secondary")
@Composable
fun CtaSecondaryPreview() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            onClick = {},
            variant = CtaButtonVariant.SECONDARY
        )
    }
}

@Preview(name = "Disabled")
@Composable
fun CtaDisabledPreview() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            onClick = {},
            enabled = false
        )
    }
}

@Preview(name = "With Icon")
@Composable
fun CtaWithIconPreview() {
    WalkItTheme {
        CtaButton(
            text = "다음으로",
            onClick = {},
            iconResId = android.R.drawable.ic_media_next // 기본 아이콘 사용
        )
    }
}

/**
 * 이전으로 버튼 (공통 컴포넌트)
 * 앱 전체에서 일관된 스타일과 너비를 유지
 */
@Composable
fun PreviousButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.wrapContentWidth(),
    enabled: Boolean = true
) {
    CtaButton(
        text = "이전으로",
        variant = CtaButtonVariant.SECONDARY,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}
