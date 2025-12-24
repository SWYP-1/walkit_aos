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

    // 사용자 정보 및 누적 통계 데이터 추출
    val (user, totalStepCount, totalDistanceMeters) = when (val state = uiState) {
        is MyPageUiState.Loading -> {
            // 계산 중일 때는 null과 0으로 표시
            Triple(null, 0, 0f)
        }
        is MyPageUiState.Success -> {
            Triple(state.user, state.totalStepCount, state.totalDistanceMeters)
        }
        is MyPageUiState.Error -> {
            // 에러 발생 시에도 사용자 정보는 표시, 통계는 0으로 표시
            Triple(state.user, 0, 0f)
        }
    }

    // 거리를 항상 km로 변환 (소수점 첫째 자리까지)
    val distanceKm = String.format("%.1f", totalDistanceMeters / 1000f)

    MyPageScreen(
        nickname = user?.nickname ?: "게스트",
        totalStepCount = totalStepCount,
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


