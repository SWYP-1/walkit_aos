package swyp.team.walkit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.GreenPrimary
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.Grey3
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 토글이 있는 메뉴 아이템 컴포넌트
 *
 * 설정 화면에서 사용하는 토글 스위치가 있는 메뉴 아이템입니다.
 * 예: "목표 알림", "푸시 알림" 등
 *
 * @param title 메뉴 제목
 * @param checked 토글 상태 (true: 켜짐, false: 꺼짐)
 * @param onCheckedChange 토글 상태 변경 핸들러
 * @param modifier Modifier
 */
@Composable
fun ToggleMenuItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: 제목 텍스트
        Text(
            text = title,
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.Normal,
            ),
            color = Grey10,
        )

        // 오른쪽: 토글 스위치
        CustomSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * 커스텀 토글 스위치
 *
 * Figma 디자인에 맞춘 커스텀 스위치입니다.
 * - 크기: 39dp x 24dp
 * - Thumb 크기: 18dp x 18dp
 * - 배경색: 켜짐 상태 - GreenPrimary (#2ABB42), 꺼짐 상태 - Grey3 (#F3F3F5)
 * - Thumb 색상: White
 */
@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 17f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "thumb_offset",
    )

    val backgroundColor = if (checked) GreenPrimary else Grey3

    Box(
        modifier = modifier
            .size(width = 39.dp, height = 24.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .offset(x = thumbOffset.dp, y = 3.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Preview
@Composable
private fun ToggleMenuItemPreview() {
    WalkItTheme {
        ToggleMenuItem(
            title = "목표 알림",
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Preview
@Composable
private fun ToggleMenuItemCheckedPreview() {
    WalkItTheme {
        ToggleMenuItem(
            title = "목표 알림",
            checked = true,
            onCheckedChange = {},
        )
    }
}

