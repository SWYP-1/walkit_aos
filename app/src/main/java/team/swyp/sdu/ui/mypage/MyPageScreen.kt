package team.swyp.sdu.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.mypage.component.MyPageAccountActions
import team.swyp.sdu.ui.mypage.component.MyPageCharacterEditButton
import team.swyp.sdu.ui.mypage.component.MyPageUserInfo
import team.swyp.sdu.ui.mypage.component.MyPageSettingsSection
import team.swyp.sdu.ui.mypage.component.MyPageStatsSection
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.WalkItTheme

/**
 * 마이 페이지 Screen
 *
 * 순수 UI 컴포넌트로, 상태와 콜백만 받습니다.
 * Preview에서 ViewModel 없이도 작동합니다.
 *
 * @param uiState 마이페이지 UI 상태
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
    uiState: MyPageUiState,
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

        when (val state = uiState) {
            is MyPageUiState.Loading -> {
                // 로딩 상태 처리 (필요시 로딩 UI 표시)
            }
            is MyPageUiState.Success -> {
                MyPageUserInfo(
                    nickname = state.user?.nickname ?: "게스트",
                    grade = state.character?.grade
                )

                MyPageCharacterEditButton(onNavigateCharacterEdit = onNavigateCharacterEdit)

                Spacer(Modifier.height(32.dp))

                HorizontalDivider(thickness = 10.dp, color = Grey3)

                Spacer(Modifier.height(32.dp))

                MyPageStatsSection(
                    totalStepCount = state.totalStepCount,
                    totalDistanceKm = totalDistanceKm
                )
            }
            is MyPageUiState.Error -> {
                MyPageUserInfo(
                    nickname = state.user?.nickname ?: "게스트",
                    grade = state.character?.grade
                )

                MyPageCharacterEditButton(onNavigateCharacterEdit = onNavigateCharacterEdit)

                Spacer(Modifier.height(32.dp))

                HorizontalDivider(thickness = 10.dp, color = Grey3)

                Spacer(Modifier.height(32.dp))

                MyPageStatsSection(
                    totalStepCount = 0,
                    totalDistanceKm = totalDistanceKm
                )
            }
        }

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
            uiState = MyPageUiState.Success(
                user = team.swyp.sdu.domain.model.User(
                    nickname = "홍길동",
                    imageName = null,
                    birthDate = null,
                    sex = null,
                ),
                character = team.swyp.sdu.domain.model.Character(
                    grade = team.swyp.sdu.data.remote.walking.dto.Grade.TREE,
                    level = 3,
                    nickName = "홍길동",
                ),
                totalStepCount = 12345,
                totalDistanceMeters = 12300f,
            ),
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