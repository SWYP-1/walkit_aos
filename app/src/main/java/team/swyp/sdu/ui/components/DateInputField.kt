package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 날짜 입력 필드 컴포넌트
 * Figma 디자인에 맞춘 텍스트 입력 필드 형태의 날짜 선택 컴포넌트
 *
 * @param value 현재 선택된 값 (표시할 텍스트)
 * @param placeholder 플레이스홀더 텍스트
 * @param modifier Modifier
 * @param onClick 클릭 시 호출되는 콜백
 */
@Composable
fun DateInputField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = SemanticColor.textBorderPrimary,
                shape = RoundedCornerShape(8.dp)
            )
            .background(SemanticColor.backgroundWhitePrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.walkItTypography.bodyM,
                color = SemanticColor.textBorderSecondary
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.walkItTypography.bodyM,
                color = SemanticColor.textBorderPrimary
            )
        }
    }
}







