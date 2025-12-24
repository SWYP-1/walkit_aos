package team.swyp.sdu.ui.mypage.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 마이 페이지 헤더 컴포넌트
 *
 * 사용자 닉네임과 등급 배지를 표시합니다.
 */
@Composable
fun MyPageUserInfo(
    nickname: String,
    grade: Grade?,
    modifier: Modifier = Modifier,
) {
    Column {
        Row(modifier = modifier) {
            Text(
                text = nickname,
                style = MaterialTheme.walkItTypography.headingM,
                color = Grey10
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "님",
                style = MaterialTheme.walkItTypography.headingM,
                color = Grey7
            )
            if (grade != null) {
                Spacer(Modifier.width(8.dp))
                GradeBadge(grade = grade)
            }
        }
    }
}





