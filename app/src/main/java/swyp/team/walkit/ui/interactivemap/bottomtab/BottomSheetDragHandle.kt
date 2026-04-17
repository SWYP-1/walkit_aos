package swyp.team.walkit.ui.interactivemap.bottomtab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 바텀시트 커스텀 드래그 핸들 — #ECECEC · 56×6dp · radius 99 */
@Composable
fun BottomSheetDragHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 6.dp)
                .background(
                    color = Color(0xFFECECEC),
                    shape = RoundedCornerShape(99.dp),
                ),
        )
    }
}
