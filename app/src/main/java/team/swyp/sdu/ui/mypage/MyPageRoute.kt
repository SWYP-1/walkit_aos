package team.swyp.sdu.ui.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import team.swyp.sdu.presentation.viewmodel.LoginViewModel

/**
 * 마이 페이지 Route
 *
 * ViewModel을 주입받고 상태를 수집하여 Screen에 전달합니다.
 */
@Composable
fun MyPageRoute(
    viewModel: MyPageViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    onNavigateCharacterEdit: () -> Unit = {},
    onNavigateUserInfoEdit: () -> Unit = {},
    onNavigateGoalManagement: () -> Unit = {},
    onNavigateNotificationSetting: () -> Unit = {},
    onNavigateMission: () -> Unit = {},
    onNavigateCustomTest: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyPageScreen(
        uiState = uiState,
        onNavigateCharacterEdit = onNavigateCharacterEdit,
        onNavigateUserInfoEdit = onNavigateUserInfoEdit,
        onNavigateGoalManagement = onNavigateGoalManagement,
        onNavigateNotificationSetting = onNavigateNotificationSetting,
        onNavigateBack = onNavigateBack,
        onNavigateMission = onNavigateMission,
        onNavigateCustomTest = onNavigateCustomTest,
        onLogout = {
            loginViewModel.logout()
            // 로그아웃 처리 후 약간의 딜레이를 두고 로그인 화면으로 이동
            // (상태 초기화가 완료될 시간을 줌)
            MainScope().launch {
                delay(100) // 100ms 딜레이
                onNavigateToLogin()
            }
        },
        onWithdraw = {
            // TODO: 탈퇴 기능 구현
        },
    )
}


