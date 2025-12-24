package team.swyp.sdu.ui.walking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onNavigateToPrevious: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: WalkingViewModel,
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
    resultViewModel: WalkingResultViewModel = hiltViewModel(),
    goalViewModel: GoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val emotionPhotoUri by viewModel.emotionPhotoUri.collectAsStateWithLifecycle()
    val resultUiState by resultViewModel.uiState.collectAsStateWithLifecycle()
    val snapshotState by viewModel.snapshotState.collectAsStateWithLifecycle()
    val currentSessionLocalId = viewModel.currentSessionLocalIdValue
    val goalState by goalViewModel.goalState.collectAsStateWithLifecycle()

    // 세션을 StateFlow로 관리
    var session by remember { mutableStateOf<WalkingSession?>(null) }
    var isLoadingSession by remember { mutableStateOf(true) }
    var sessionError by remember { mutableStateOf<String?>(null) }

    // 하이브리드 접근: 우선 Completed 상태의 세션 사용 (메모리에서 즉시 접근),
    // 없으면 DB에서 조회 (Fallback)
    LaunchedEffect(uiState, currentSessionLocalId) {
        when (val state = uiState) {
            is WalkingUiState.Completed -> {
                session = state.session
                isLoadingSession = false
                sessionError = null
                Timber.d("세션 로드 완료 (Completed 상태 - 메모리에서): ${state.session}, localId=$currentSessionLocalId")
            }
            else -> {
                val localId = currentSessionLocalId
                if (localId != null) {
                    try {
                        isLoadingSession = true
                        sessionError = null
                        val loadedSession = viewModel.getSessionById(localId)
                        if (loadedSession != null) {
                            session = loadedSession
                            Timber.d("세션 로드 완료 (DB에서 조회): localId=$localId")
                        } else {
                            sessionError = "세션을 찾을 수 없습니다 (ID: $localId)"
                            Timber.e("세션을 찾을 수 없습니다: localId=$localId")
                        }
                    } catch (e: Exception) {
                        sessionError = "세션 로드 중 오류 발생: ${e.message}"
                        Timber.e(e, "세션 로드 실패: localId=$localId")
                    } finally {
                        isLoadingSession = false
                    }
                } else {
                    sessionError = "세션 ID가 없습니다"
                    Timber.e("WalkingResultRoute에 도달했지만 세션 ID가 없습니다. 상태: $state")
                    isLoadingSession = false
                }
            }
        }
    }

    // 사진이 없을 때만 맵뷰에 locations 전달
    LaunchedEffect(locations, emotionPhotoUri) {
        if (locations.isNotEmpty() && emotionPhotoUri == null) {
            mapViewModel.setLocations(locations)
        }
    }

    // ViewModel 정보 로깅 (디버깅용)
    LaunchedEffect(viewModel, emotionPhotoUri, locations) {
        Timber.d("🚶 WalkingResultRoute ViewModel 상태:")
        Timber.d("  📸 emotionPhotoUri: $emotionPhotoUri")
        Timber.d("  📍 locations: ${locations.size}개")
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
        onNavigateToPrevious = onNavigateToPrevious,
        onNavigateToHome = onNavigateToHome,
        currentSession = session,
        isLoadingSession = isLoadingSession,
        sessionError = sessionError,
        locations = locations,
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
        onUpdateNote = { localId, note ->
            viewModel.updateSessionNote(localId, note)
        },
        onDeleteNote = { localId ->
            viewModel.deleteSessionNote(localId)
        },
        currentSessionLocalId = currentSessionLocalId,
    )
}

