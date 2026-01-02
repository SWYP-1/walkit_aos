package team.swyp.sdu.ui.walking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.presentation.viewmodel.GoalViewModel
import team.swyp.sdu.presentation.viewmodel.KakaoMapViewModel
import team.swyp.sdu.ui.walking.components.CustomProgressIndicator
import team.swyp.sdu.ui.walking.components.WalkingResultCompletionDialog
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

    // 목표 데이터 명시적 로드
    LaunchedEffect(Unit) {
        Timber.d("🎯 WalkingResultRoute: 목표 데이터 로드 시작")
        goalViewModel.refreshGoal()
    }

    // ✅ Flow로 세션 관찰 (자동 갱신)
    val session by viewModel.currentSession.collectAsStateWithLifecycle()

    // 산책 완료 다이얼로그 표시 상태
    var showWalkingCompleteDialog by remember { mutableStateOf(false) }

    // 저장 중일 때만 로딩 표시
//    if (uiState is WalkingUiState.SavingSession) {
//        Box(
//            modifier = modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            CustomProgressIndicator()
//        }
//        return
//    }

    // 동기화 완료 시 다이얼로그 표시, 실패 시 바로 홈 이동
    LaunchedEffect(snapshotState) {
        when (snapshotState) {
            SnapshotState.Complete -> {
                Timber.d("🚶 산책 동기화 성공: 다이얼로그 표시")
                showWalkingCompleteDialog = true
            }
            is SnapshotState.Error -> {
                Timber.d("🚶 산책 동기화 실패로 홈으로 이동: $snapshotState")
                onNavigateToHome()
            }
            else -> Unit // 다른 상태에서는 아무 동작도 하지 않음
        }
    }

    // ✅ session이 null이면 로딩, 있으면 화면 표시
    if (session == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CustomProgressIndicator()
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
            Timber.d("session id : ${session?.id}")
            Timber.d("session note : ${session?.note}")
            Timber.d("session pre emo : ${session?.preWalkEmotion?.name}")
            Timber.d("session post emo : ${session?.postWalkEmotion?.name}")
            Timber.d("session localImagePath : ${session?.localImagePath}")
            Timber.d("session locations : ${session?.locations}")
            Timber.d("session stepCount : ${session?.stepCount}")
            Timber.d("  📸 emotionPhotoUri: $emotionPhotoUri")
            Timber.d("  📍 session.locations: ${session?.locations?.size ?: 0}개")
            Timber.d("  🎯 emotionText: ${viewModel.emotionText.value}")
            Timber.d("  📊 uiState: ${viewModel.uiState.value}")
        }

        // Goal 데이터 추출 및 ViewModel에 설정
        val goal: Goal? = when (val goal = goalState) {
            is team.swyp.sdu.core.Result.Success -> goal.data
            else -> null
        }

        // Goal 상태 로깅 (디버깅용)
        LaunchedEffect(goalState, goal) {
            Timber.d("🎯 WalkingResultRoute Goal 상태:")
            Timber.d("  📊 goalState: $goalState")
            Timber.d("  🎯 goal: $goal")
            Timber.d("  📈 targetStepCount: ${goal?.targetStepCount}")
        }

        // WalkingViewModel에 현재 goal 설정 (targetStepCount 저장용)
        LaunchedEffect(goal) {
            Timber.d("🎯 WalkingResultRoute: setCurrentGoal 호출 - goal=$goal")
            viewModel.setCurrentGoal(goal)
        }

        // 이번주 동기화된 세션 목록 추출 (이미 ViewModel에서 SYNCED 필터링됨)
        val syncedSessionsThisWeek = (resultUiState as? WalkingResultUiState.Success)
            ?.syncedSessionsThisWeek
            .orEmpty()

        WalkingResultScreen(
            modifier = modifier,
            onNavigateToPrevious = onNavigateToPrevious,
            onNavigateToHome = onNavigateToHome,
            currentSession = session,
            weekWalkOrder = syncedSessionsThisWeek.size,
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

        // 산책 완료 다이얼로그
        if (showWalkingCompleteDialog) {
            WalkingResultCompletionDialog(
                onConfirm = {
                    showWalkingCompleteDialog = false
                    onNavigateToHome()
                }
            )
        }
    }
}