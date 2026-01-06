package swyp.team.walkit.ui.mypage.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.components.MenuItem
import swyp.team.walkit.ui.components.SectionCard
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 마이 페이지 설정 섹션 컴포넌트
 *
 * 알림 설정, 내 정보 관리, 내 목표 관리를 포함합니다.
 */
@Composable
fun MyPageSettingsSection(
    onNavigateNotificationSetting: () -> Unit,
    onNavigateUserInfoEdit: () -> Unit,
    onNavigateGoalManagement: () -> Unit,
    onNavigateMission : () -> Unit,
    onNavigateCustomTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = 16.dp
    val verticalPadding = 12.dp
    Column() {
        // 일반 설정 섹션
        SectionCard(modifier = modifier) {
            Text(
                text = "설정",
                style = MaterialTheme.walkItTypography.bodyL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Grey10,
                modifier = Modifier.padding(top = verticalPadding, start = horizontalPadding)
            )
            Spacer(Modifier.height(8.dp))

            MenuItem("알람 설정", onNavigateNotificationSetting)
            Spacer(Modifier.height(8.dp))
            MenuItem("내 정보 관리", onNavigateUserInfoEdit)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        // 추가 설정 섹션
        SectionCard {
            Text(
                text = "목표",
                style = MaterialTheme.walkItTypography.bodyL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Grey10,
                modifier = Modifier.padding(top = verticalPadding, start = horizontalPadding)
            )
            Spacer(Modifier.height(8.dp))
            MenuItem("내 목표", onNavigateGoalManagement)
            Spacer(Modifier.height(8.dp))
            MenuItem("내 미션",onNavigateMission)
//            Spacer(Modifier.height(8.dp))
            MenuItem("커스텀 테스트", onNavigateCustomTest)
            Spacer(Modifier.height(12.dp))
        }
    }

}

@Preview
@Composable
fun MypageSettingSectionPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        MyPageSettingsSection(
            onNavigateNotificationSetting = {},
            onNavigateUserInfoEdit = {},
            onNavigateGoalManagement = {},
            onNavigateMission = {},
            onNavigateCustomTest = {}
        )
    }
}








