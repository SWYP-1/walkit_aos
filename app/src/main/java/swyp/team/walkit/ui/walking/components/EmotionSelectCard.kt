package swyp.team.walkit.ui.walking.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun EmotionSelectCard(
    emotionText: String,
    textColor: Color,
    drawableId: Int,
    boxColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        alpha = (0.15f * 255).toInt() // 15%
                        maskFilter = BlurMaskFilter(
                            16f, // 🔥 blur = 12
                            BlurMaskFilter.Blur.NORMAL
                        )
                    }

                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        12.dp.toPx(),
                        12.dp.toPx(),
                        paint
                    )
                }
            }
            .background(
                if (isSelected) boxColor else SemanticColor.backgroundWhitePrimary,
                shape = RoundedCornerShape(12.dp) // 🔥 여기서 shape 처리
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = textColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                indication = null, // 🔥 ripple 제거
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            }
            .padding(16.dp)
    ) {
        Text(
            text = emotionText, style = MaterialTheme.walkItTypography.bodyXL.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )
        Image(
            painter = painterResource(drawableId),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.BottomEnd),
        )
    }
}
