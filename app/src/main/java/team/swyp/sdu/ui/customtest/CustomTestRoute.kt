package team.swyp.sdu.ui.customtest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 커스텀 테스트 Route
 *
 * 네비게이션과 상태 관리를 담당합니다.
 */
@Composable
fun CustomTestRoute(
    modifier: Modifier = Modifier,
    viewModel: CustomTestViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onStartOnboarding: () -> Unit = {},
    onNavigateToMapTest: () -> Unit = {},
    onNavigateToGalleryTest: () -> Unit = {},
) {
    val onAddDummySessions: () -> Unit = {
        viewModel.addDummySessions()
    }

    CustomTestScreen(
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        onStartOnboarding = onStartOnboarding,
        onAddDummySessions = onAddDummySessions,
        onNavigateToMapTest = onNavigateToMapTest,
        onNavigateToGalleryTest = onNavigateToGalleryTest,
    )
}
