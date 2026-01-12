package swyp.team.walkit.ui.mypage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.Grey3
import swyp.team.walkit.ui.theme.Grey7
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 마이 페이지 계정 액션 컴포넌트
 *
 * 로그아웃과 탈퇴하기를 표시합니다.
 */
@Composable
fun MyPageAccountActions(
    onLogout: () -> Unit = {},
    onWithdraw: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val buttonShape = RoundedCornerShape(12.dp)

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
    ) {
        ActionTextButton(
            text = "로그아웃",
            onClick = onLogout,
            shape = buttonShape
        )

        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .height(16.dp),
            thickness = 1.dp,
            color = Grey3
        )

        ActionTextButton(
            text = "탈퇴 하기",
            onClick = onWithdraw,
            shape = buttonShape
        )
    }
}

@Composable
private fun ActionTextButton(
    text: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = Modifier
            .clip(shape)                 // ✅ 클릭 영역 라운드
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp), // 터치 영역 확보
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.walkItTypography.bodyS,
            color = Grey7
        )
    }
}
