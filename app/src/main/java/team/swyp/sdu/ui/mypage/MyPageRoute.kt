package team.swyp.sdu.ui.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 거리를 항상 km로 변환 (소수점 첫째 자리까지)
    val distanceKm = when (val state = uiState) {
        is MyPageUiState.Success -> String.format("%.1f", state.totalDistanceMeters / 1000f)
        is MyPageUiState.Error -> String.format("%.1f", 0f)
        is MyPageUiState.Loading -> "0.0"
    }

    MyPageScreen(
        uiState = uiState,
        totalDistanceKm = distanceKm,
        onNavigateCharacterEdit = onNavigateCharacterEdit,
        onNavigateUserInfoEdit = onNavigateUserInfoEdit,
        onNavigateGoalManagement = onNavigateGoalManagement,
        onNavigateNotificationSetting = onNavigateNotificationSetting,
        onNavigateBack = onNavigateBack,
        onLogout = {
            loginViewModel.logout()
            onNavigateToLogin()
        },
        onWithdraw = {
            // TODO: 탈퇴 기능 구현
        },
    )
}


