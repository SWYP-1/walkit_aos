package team.swyp.sdu.ui.walking

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.presentation.viewmodel.GoalViewModel
import team.swyp.sdu.presentation.viewmodel.KakaoMapViewModel
import team.swyp.sdu.ui.walking.viewmodel.WalkingResultUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingResultViewModel
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.walking.viewmodel.SnapshotState

/**
 * 산책 결과 화면 Route
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun WalkingResultRoute(
    modifier : Modifier = Modifier,
    onNavigateToPrevious: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: WalkingViewModel,
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
    resultViewModel: WalkingResultViewModel = hiltViewModel(),
    goalViewModel: GoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emotionPhotoUri by viewModel.emotionPhotoUri.collectAsStateWithLifecycle()
    val resultUiState by resultViewModel.uiState.collectAsStateWithLifecycle()
    val snapshotState by viewModel.snapshotState.collectAsStateWithLifecycle()
    val goalState by goalViewModel.goalState.collectAsStateWithLifecycle()

    // ✅ Flow로 세션 관찰 (자동 갱신)
    val session by viewModel.currentSession.collectAsStateWithLifecycle()

    // 저장 중일 때만 로딩 표시
    if (uiState is WalkingUiState.SavingSession) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // ✅ session이 null이면 로딩, 있으면 화면 표시
    if (session == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // 사진이 없을 때만 맵뷰에 session.locations 전달
        LaunchedEffect(session, emotionPhotoUri) {
            val currentSession = session
            if (currentSession != null && currentSession.locations.isNotEmpty() && emotionPhotoUri == null) {
                mapViewModel.setLocations(currentSession.locations)
            }
        }

        // ViewModel 정보 로깅 (디버깅용)
        LaunchedEffect(viewModel, emotionPhotoUri, session) {
            Timber.d("🚶 WalkingResultRoute ViewModel 상태:")
            Timber.d("  📸 emotionPhotoUri: $emotionPhotoUri")
            Timber.d("  📍 session.locations: ${session?.locations?.size ?: 0}개")
            Timber.d("  🎯 emotionText: ${viewModel.emotionText.value}")
            Timber.d("  📊 uiState: ${viewModel.uiState.value}")
        }

    // Goal 데이터 추출
    val goal: Goal? = when (val goal = goalState) {
        is team.swyp.sdu.core.Result.Success -> goal.data
        else -> null
    }

    // 이번주 동기화된 세션 목록 추출
    val syncedSessionsThisWeek = (resultUiState as? WalkingResultUiState.Success)
        ?.sessionsThisWeek
        ?.filter { session ->
            // TODO: SYNCED 상태인 세션만 필터링 (현재는 모든 세션 사용)
            true // 임시로 모든 세션 사용
        }
        .orEmpty()

        WalkingResultScreen(
            modifier = modifier,
            onNavigateToPrevious = onNavigateToPrevious,
            onNavigateToHome = onNavigateToHome,
            currentSession = session!!, // Flow에서 자동으로 최신 데이터 제공
            isLoadingSession = false, // Flow 사용으로 로딩 불필요
            sessionError = null, // 에러는 Flow에서 null로 처리
            emotionPhotoUri = emotionPhotoUri,
            goal = goal,
            syncedSessionsThisWeek = syncedSessionsThisWeek,
            snapshotState = snapshotState,
            onCaptureSnapshot = { captureSnapshotCallback ->
                viewModel.captureAndSaveSnapshot(captureSnapshotCallback)
            },
            onSyncSessionToServer = {
                viewModel.syncSessionToServer()
            },
            onDeleteNote = { localId ->
                viewModel.deleteSessionNote(localId)
            },
        )
}

