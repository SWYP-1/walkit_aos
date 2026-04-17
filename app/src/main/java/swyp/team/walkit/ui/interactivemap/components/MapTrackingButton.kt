package swyp.team.walkit.ui.interactivemap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.interactivemap.MapTrackingMode
import swyp.team.walkit.ui.theme.SemanticColor

/**
 * 지도 위치 추적 버튼 (나침반)
 *
 * IDLE     → 아이콘 회색 (추적 안 함)
 * FOLLOWING → 아이콘 초록 (위치 추적 중)
 * COMPASS  → 아이콘 초록 + 방향 회전 (방향 추적 중)
 *
 * @param trackingMode 현재 추적 모드
 * @param onClick      버튼 클릭 콜백
 */
@Composable
fun MapTrackingButton(
    trackingMode: MapTrackingMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconTint = when (trackingMode) {
        MapTrackingMode.IDLE -> SemanticColor.iconGrey
        MapTrackingMode.FOLLOWING -> SemanticColor.iconGreen
    }

    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = CircleShape)
            .size(33.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_map_tracking),
            contentDescription = when (trackingMode) {
                MapTrackingMode.IDLE -> "위치 추적 시작"
                MapTrackingMode.FOLLOWING -> "위치 추적 종료"
            },
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}
