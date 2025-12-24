package team.swyp.sdu.ui.mypage.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.walkItTypography

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
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "로그아웃",
            style = MaterialTheme.walkItTypography.bodyS,
            color = Grey7,
            modifier = Modifier.clickable(onClick = onLogout)
        )

        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .height(16.dp),
            thickness = 1.dp,
            color = Grey3
        )

        Text(
            text = "탈퇴 하기",
            style = MaterialTheme.walkItTypography.bodyS,
            color = Grey7,
            modifier = Modifier.clickable(onClick = onWithdraw)
        )
    }
}


