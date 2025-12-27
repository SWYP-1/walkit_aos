package team.swyp.sdu.ui.walking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel

/**
 * 산책 전 감정 선택 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun PreWalkingEmotionSelectRoute(
    viewModel: WalkingViewModel = hiltViewModel(),
    onPrev: () -> Unit,
    onNext: () -> Unit,
    permissionsGranted: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedEmotion = when (uiState) {
        is WalkingUiState.PreWalkingEmotionSelection -> (uiState as WalkingUiState.PreWalkingEmotionSelection).preWalkingEmotion
        else -> null
    }

    PreWalkingEmotionSelectScreen(
        selectedEmotion = selectedEmotion,
        permissionsGranted = permissionsGranted,
        onEmotionSelected = viewModel::selectPreWalkingEmotion,
        onPrev = onPrev,
        onNext = onNext,
    )
}
