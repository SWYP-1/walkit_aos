package team.swyp.sdu.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.mypage.component.MyPageAccountActions
import team.swyp.sdu.ui.mypage.component.MyPageCharacterEditButton
import team.swyp.sdu.ui.mypage.component.MyPageHeader
import team.swyp.sdu.ui.mypage.component.MyPageSettingsSection
import team.swyp.sdu.ui.mypage.component.MyPageStatsSection
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 마이 페이지 Screen
 *
 * 순수 UI 컴포넌트로, 상태와 콜백만 받습니다.
 * Preview에서 ViewModel 없이도 작동합니다.
 *
 * @param nickname 사용자 닉네임
 * @param totalStepCount 총 걸음수
 * @param totalDistanceKm 총 이동거리 (km, 포맷된 문자열)
 * @param onNavigateCharacterEdit 캐릭터 정보 수정 네비게이션 핸들러
 * @param onNavigateUserInfoEdit 내 정보 관리 네비게이션 핸들러
 * @param onNavigateNotificationSetting 알림 설정 네비게이션 핸들러
 * @param onNavigateBack 뒤로가기 핸들러
 * @param onLogout 로그아웃 핸들러
 * @param onWithdraw 탈퇴하기 핸들러
 * @param modifier Modifier
 */
@Composable
fun MyPageScreen(
    nickname: String,
    totalStepCount: Int,
    totalDistanceKm: String,
    onNavigateCharacterEdit: () -> Unit,
    onNavigateUserInfoEdit: () -> Unit,
    onNavigateGoalManagement: () -> Unit,
    onNavigateNotificationSetting: () -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    onWithdraw: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppHeader(
            title = "마이 페이지",
            onNavigateBack = onNavigateBack,
        )

        MyPageHeader(nickname = nickname)

        MyPageCharacterEditButton(onNavigateCharacterEdit = onNavigateCharacterEdit)

        Spacer(Modifier.height(32.dp))

        HorizontalDivider(thickness = 10.dp, color = Grey3)

        Spacer(Modifier.height(32.dp))

        MyPageStatsSection(
            totalStepCount = totalStepCount,
            totalDistanceKm = totalDistanceKm
        )

        Spacer(Modifier.height(32.dp))

        MyPageSettingsSection(
            onNavigateNotificationSetting = onNavigateNotificationSetting,
            onNavigateUserInfoEdit = onNavigateUserInfoEdit,
            onNavigateGoalManagement = onNavigateGoalManagement
        )

        Spacer(Modifier.height(32.dp))

        MyPageAccountActions(
            onLogout = onLogout,
            onWithdraw = onWithdraw,
        )
    }
}


@Preview
@Composable
private fun MyPageScreenPreview() {
    WalkItTheme {
        MyPageScreen(
            nickname = "홍길동",
            totalStepCount = 12345,
            totalDistanceKm = "12.3",
            onNavigateCharacterEdit = {},
            onNavigateUserInfoEdit = {},
            onNavigateGoalManagement = {},
            onNavigateNotificationSetting = {},
            onNavigateBack = {},
            onLogout = {},
            onWithdraw = {},
        )
    }
}