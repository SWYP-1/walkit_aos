package team.swyp.sdu.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import team.swyp.sdu.ui.theme.Green4
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.White
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * CTA 버튼 컴포넌트
 *
 * Figma 디자인 기반 공용 버튼
 * - 기본 상태: 녹색 배경 (#52ce4b)
 * - 눌린 상태: 동일한 디자인 (Material3 기본 pressed 상태)
 *
 * @param text 버튼 텍스트
 * @param onClick 클릭 이벤트 핸들러
 * @param modifier Modifier
 * @param enabled 버튼 활성화 여부 (기본값: true)
 * @param icon 아이콘 (선택사항, 향후 확장 가능)
 */
@Composable
fun CtaButton(
    text: String,
    textColor: Color = SemanticColor.textBorderPrimaryInverse,
    buttonColor: Color = SemanticColor.buttonPrimaryDefault,
    buttonHeight : Dp = 47.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Figma 디자인 색상
    val disabledColor = Color(0xFFE0E0E0) // 비활성화 색상 (임시)
    val borderWidth = if (buttonColor == SemanticColor.buttonPrimaryDefault) 1.dp else 0.dp

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = SemanticColor.textBorderGreenPrimary, shape = RoundedCornerShape(8.dp))
            .height(buttonHeight),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) buttonColor else disabledColor,
            contentColor = textColor,
            disabledContainerColor = disabledColor,
            disabledContentColor = Color(0xFF9E9E9E),
        ),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 10.dp,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPressed) 0.dp else 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor,
                maxLines = 1
            )

            icon?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }
    }
}

/**
 * CtaButton Preview
 */
@Preview(name = "흰색 글자 + 초록 바탕 (아이콘 없음)")
@Composable
fun CtaButtonPreview1() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            textColor = White,
            buttonColor = Green4,
            onClick = {},
        )
    }

}

@Preview(name = "초록 글자 + 흰색 바탕 (아이콘 없음)")
@Composable
fun CtaButtonPreview2() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            textColor = Green4,
            buttonColor = White,
            onClick = {},
        )
    }

}

@Preview(name = "흰색 글자 + 초록 바탕 (아이콘 있음)")
@Composable
fun CtaButtonPreview3() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            textColor = White,
            buttonColor = Green4,
            onClick = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = White,
                )
            },
        )
    }

}

@Preview(name = "초록 글자 + 흰색 바탕 (아이콘 있음)")
@Composable
fun CtaButtonPreview4() {
    WalkItTheme {
        CtaButton(
            text = "CTA button",
            textColor = Green4,
            buttonColor = White,
            onClick = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Green4,
                )
            },
        )
    }
}

