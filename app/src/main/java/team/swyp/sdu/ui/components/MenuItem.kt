package team.swyp.sdu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.TypeScale
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 마이페이지 메뉴 아이템 컴포넌트
 *
 * 재사용 가능한 메뉴 아이템으로, 제목만 변경하여 사용할 수 있습니다.
 * 예: "내 정보 설정", "알림 설정", "약관" 등
 *
 * @param title 메뉴 제목
 * @param onClick 클릭 이벤트 핸들러
 * @param modifier Modifier
 */
@Composable
fun MenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: 제목 텍스트
        Text(
            text = title,
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = Grey10
        )

        // 오른쪽: chevron_right 아이콘
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF818185), // color/icon/grey
            modifier = Modifier.size(24.dp),
        )
    }
}

