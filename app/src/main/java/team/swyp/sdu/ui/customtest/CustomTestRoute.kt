package team.swyp.sdu.ui.customtest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 커스텀 테스트 Route
 *
 * 네비게이션과 상태 관리를 담당합니다.
 */
@Composable
fun CustomTestRoute(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onStartOnboarding: () -> Unit = {},
) {
    CustomTestScreen(
        onNavigateBack = onNavigateBack,
        onStartOnboarding = onStartOnboarding,
    )
}
