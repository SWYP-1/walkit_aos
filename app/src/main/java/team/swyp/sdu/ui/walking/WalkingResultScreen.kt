package team.swyp.sdu.ui.walking

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.KakaoMapView
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.components.captureMapViewSnapshot
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.presentation.viewmodel.KakaoMapViewModel
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.walking.components.GoalProgressCard
import team.swyp.sdu.ui.walking.components.StatItem
import team.swyp.sdu.ui.walking.components.WeekCompletionRow
import team.swyp.sdu.ui.walking.components.PathThumbnail
import team.swyp.sdu.ui.walking.components.formatDuration
import team.swyp.sdu.ui.walking.components.formatDistance
import team.swyp.sdu.ui.walking.components.WalkingResultCompletionDialog
import team.swyp.sdu.ui.walking.components.WalkingResultLoadingOverlay
import team.swyp.sdu.ui.walking.viewmodel.SnapshotState
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.SummaryUnit
import team.swyp.sdu.ui.components.WalkingSummaryCard
import team.swyp.sdu.ui.record.components.WalkingDiaryCard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.walking.components.WalkingProgressBar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * 사진 + 경로 스냅샷 캡처 (suspend 함수로 구현)
 *
 * Compose에서 코루틴을 사용하는 방법:
 * - rememberCoroutineScope()로 scope 얻기
 * - launch { } 블록에서 suspend 함수 호출
 * - 콜백 기반 API는 suspendCancellableCoroutine으로 변환 (취소 가능, 권장)
 */
private suspend fun capturePhotoWithPathSnapshot(
    coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
    context: android.content.Context,
): String? {
    if (coordinates == null) {
        Timber.w("사진+경로 Box 위치 정보가 없습니다")
        return null
    }

    val width = coordinates.size.width.toInt()
    val height = coordinates.size.height.toInt()

    if (width <= 0 || height <= 0) {
        Timber.w("Box 크기가 0입니다: ${width}x${height}")
        return null
    }

    val activity = context as? android.app.Activity
    if (activity == null) {
        Timber.w("Activity를 찾을 수 없습니다")
        return null
    }

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
        Timber.w("PixelCopy는 Android 8.0 이상에서만 사용 가능")
        return null
    }

    val boundsInWindow = coordinates.boundsInWindow()
    val window = activity.window

    val bitmap = android.graphics.Bitmap.createBitmap(
        width,
        height,
        android.graphics.Bitmap.Config.ARGB_8888
    )

    val rect = android.graphics.Rect(
        boundsInWindow.left.toInt(),
        boundsInWindow.top.toInt(),
        boundsInWindow.right.toInt(),
        boundsInWindow.bottom.toInt()
    )

    // PixelCopy의 콜백을 suspend 함수로 변환
    // suspendCancellableCoroutine은 취소 가능한 코루틴으로, suspendCoroutine보다 권장됨
    return suspendCancellableCoroutine { continuation ->
        android.view.PixelCopy.request(
            window,
            rect,
            bitmap,
            { copyResult ->
                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    Timber.d("사진+경로 PixelCopy 스냅샷 생성 완료: ${bitmap.width}x${bitmap.height}")
                    val savedPath = saveSnapshotToFile(context, bitmap)
                    Timber.d("사진+경로 스냅샷 파일 저장: $savedPath")
                    continuation.resume(savedPath) {}
                } else {
                    Timber.e("사진+경로 PixelCopy 실패: $copyResult")
                    continuation.resume(null) {}
                }
            },
            android.os.Handler(android.os.Looper.getMainLooper())
        )

        // 코루틴이 취소되면 PixelCopy 요청도 취소할 수 있도록 설정
        continuation.invokeOnCancellation {
            Timber.d("PixelCopy 요청 취소됨")
            // PixelCopy는 취소할 수 없지만, 로깅은 가능
        }
    }
}

/**
 * 스냅샷을 파일로 저장
 */
private fun saveSnapshotToFile(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap,
): String? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "walking_snapshot_${timestamp}.png"

        val fileDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(fileDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }

        val absolutePath = file.absolutePath
        Timber.d("스냅샷 파일 저장 완료: $absolutePath")
        absolutePath
    } catch (e: Exception) {
        Timber.e(e, "스냅샷 파일 저장 실패: ${e.message}")
        null
    }
}

/**
 * 산책 결과 화면 Screen
 * 상태 처리(Loading, Success, Error)를 담당합니다.
 */
@Composable
fun WalkingResultScreen(
    onNavigateToPrevious: () -> Unit,
    onNavigateToHome: () -> Unit,
    currentSession: WalkingSession?,
    isLoadingSession: Boolean,
    sessionError: String?,
    emotionPhotoUri: android.net.Uri?,
    goal: Goal?,
    syncedSessionsThisWeek: List<WalkingSession>,
    snapshotState: SnapshotState,
    onCaptureSnapshot: suspend (suspend () -> String?) -> Boolean,
    onSyncSessionToServer: () -> Unit,
    onUpdateNote: (Long, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    currentSessionLocalId: Long?,
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
) {
    when {
        isLoadingSession -> {
            // 로딩 상태
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
            }
        }

        currentSession == null -> {
            // 에러 상태
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = sessionError ?: "세션 정보를 불러올 수 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = onNavigateToPrevious) {
                        Text("돌아가기")
                    }
                }
            }
        }

        else -> {
            // 성공 상태
            WalkingResultScreenContent(
                onNavigateToPrevious = onNavigateToPrevious,
                onNavigateToHome = onNavigateToHome,
                currentSession = currentSession,
                emotionPhotoUri = emotionPhotoUri,
                goal = goal,
                syncedSessionsThisWeek = syncedSessionsThisWeek,
                snapshotState = snapshotState,
                onCaptureSnapshot = onCaptureSnapshot,
                onSyncSessionToServer = onSyncSessionToServer,
                onUpdateNote = onUpdateNote,
                onDeleteNote = onDeleteNote,
                currentSessionLocalId = currentSessionLocalId,
                mapViewModel = mapViewModel,
            )
        }
    }
}

/**
 * 산책 결과 화면 Content
 * 실제 UI 컴포넌트를 렌더링합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalkingResultScreenContent(
    onNavigateToPrevious: () -> Unit,
    onNavigateToHome: () -> Unit,
    currentSession: WalkingSession,
    emotionPhotoUri: android.net.Uri?,
    goal: Goal?,
    syncedSessionsThisWeek: List<WalkingSession>,
    snapshotState: SnapshotState,
    onCaptureSnapshot: suspend (suspend () -> String?) -> Boolean,
    onSyncSessionToServer: () -> Unit,
    onUpdateNote: (Long, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    currentSessionLocalId: Long?,
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 사진 + 경로 Box의 위치 정보 (스냅샷 생성용)
    var photoWithPathBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // 지도 MapView 참조 (스냅샷 생성용)
    var mapViewRef by remember { mutableStateOf<com.kakao.vectormap.MapView?>(null) }

    // 완료 팝업 표시 여부
    var showCompletionDialog by remember { mutableStateOf(false) }

    // 노트 수정 다이얼로그 표시 여부
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editNoteText by remember { mutableStateOf(currentSession.note ?: "") }

    // 서버 동기화 완료 시 팝업 표시
    LaunchedEffect(snapshotState) {
        if (snapshotState is SnapshotState.Complete) {
            showCompletionDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
            // 진행 바 (1번째 칸 채워짐)
            item {
                WalkingProgressBar(
                    currentStep = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = "오늘도 산책 완료!",
                    // heading S/semibold
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimary,
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("이번 주 ")

                        withStyle(
                            style = SpanStyle(color = SemanticColor.stateAquaBluePrimary)
                        ) {
                            append("N번째")
                        }

                        append(" 산책을 완료했어요.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SemanticColor.textBorderSecondary, // 기본 색
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                // 사진이 있는 경우에만 위치 정보 저장 (스냅샷 생성용)
                                if (emotionPhotoUri != null) {
                                    photoWithPathBoxCoordinates = coordinates
                                }
                            }
                    ) {
                        if (emotionPhotoUri != null) {
                            // 케이스 1: 사진이 있는 경우 - 사진 + 경로만 표시 (맵뷰 로딩하지 않음)
                            val bitmap = remember(emotionPhotoUri) {
                                try {
                                    val inputStream =
                                        context.contentResolver.openInputStream(emotionPhotoUri!!)
                                    android.graphics.BitmapFactory.decodeStream(inputStream)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "산책 사진",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }

                            // 경로 표시
                            PathThumbnail(
                                locations = currentSession.locations,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                pathColor = Color(0xFF2196F3),
                                endpointColor = Color(0xFF2196F3),
                            )
                        } else {
                            // 케이스 2: 사진이 없는 경우 - MapView 직접 표시
                            KakaoMapView(
                                locations = currentSession.locations,
                                modifier = Modifier.fillMaxSize(),
                                viewModel = mapViewModel,
                                onMapViewReady = { mapView ->
                                    // MapView 참조 저장 (스냅샷 생성용)
                                    mapViewRef = mapView
                                },
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                WalkingSummaryCard(
                    leftValue = currentSession.stepCount.toString(),
                    leftLabel = "걸음 수",
                    leftUnit = SummaryUnit.Step("걸음"),
                    rightLabel = "산책 시간",
                    rightUnit = SummaryUnit.Time(currentSession.duration),
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 목표 진행률 카드
            goal?.let {
                item {
                    GoalProgressCard(
                        goal = it,
                        currentSession = currentSession,
                        syncedSessionsThisWeek = syncedSessionsThisWeek,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                WalkingDiaryCard(
                    session = currentSession,
                    modifier = Modifier.fillMaxWidth(),
                    onEditClick = {
                        editNoteText = currentSession.note ?: ""
                        showEditNoteDialog = true
                    },
                    onDeleteClick = {
                        currentSessionLocalId?.let { localId ->
                            onDeleteNote(localId)
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CtaButton(
                            text = "이전으로",
                            textColor = SemanticColor.buttonPrimaryDefault,
                            buttonColor = SemanticColor.backgroundWhitePrimary,
                            onClick = onNavigateToPrevious,
                            modifier = Modifier.width(96.dp),
                            enabled = snapshotState is SnapshotState.Idle ||
                                    snapshotState is SnapshotState.Error,
                        )

                        CtaButton(
                            text = "저장하기",
                            textColor = SemanticColor.textBorderPrimaryInverse,
                            buttonColor = SemanticColor.buttonPrimaryDefault,
                            onClick = {
                                coroutineScope.launch {
                                    // 스냅샷 생성 및 저장
                                    val success = onCaptureSnapshot {
                                        try {
                                            if (emotionPhotoUri != null) {
                                                // 케이스 1: 사진 + 경로 스냅샷 (맵뷰 로딩 없음)
                                                capturePhotoWithPathSnapshot(
                                                    photoWithPathBoxCoordinates,
                                                    context
                                                )
                                            } else {
                                                // 케이스 2: 지도 + 경로 스냅샷
                                                // MapView를 PixelCopy로 캡처
                                                if (mapViewRef != null) {
                                                    captureMapViewSnapshot(mapViewRef!!, context)
                                                } else {
                                                    Timber.w("MapView 참조가 없습니다 - 스냅샷 생성 실패")
                                                    null
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Timber.e(e, "스냅샷 생성 실패")
                                            null
                                        }
                                    }

                                    // 스냅샷 저장 완료 후 서버 동기화 시작
                                    if (success) {
                                        onSyncSessionToServer()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = snapshotState is SnapshotState.Idle ||
                                    snapshotState is SnapshotState.Error,
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 저장 중 오버레이
        if (snapshotState is SnapshotState.Capturing ||
            snapshotState is SnapshotState.Saving ||
            snapshotState is SnapshotState.Syncing
        ) {
            WalkingResultLoadingOverlay()
        }

        // 완료 팝업
        if (showCompletionDialog) {
            WalkingResultCompletionDialog(
                onConfirm = {
                    showCompletionDialog = false
                    onNavigateToHome()
                },
            )
        }

        // 노트 수정 다이얼로그
        if (showEditNoteDialog) {
            AlertDialog(
                onDismissRequest = { showEditNoteDialog = false },
                title = {
                    Text(text = "노트 수정")
                },
                text = {
                    OutlinedTextField(
                        value = editNoteText,
                        onValueChange = { editNoteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("노트를 입력하세요") },
                        maxLines = 5,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentSessionLocalId?.let { localId ->
                                val noteToSave = editNoteText.ifEmpty { null }
                                if (noteToSave != null) {
                                    onUpdateNote(localId, noteToSave)
                                } else {
                                    onDeleteNote(localId)
                                }
                            }
                            showEditNoteDialog = false
                        },
                    ) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditNoteDialog = false },
                    ) {
                        Text("취소")
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun WalkingResultScreenPreview() {
    WalkItTheme {
        val mockSession = WalkingSession(
            id = "preview-session",
            startTime = System.currentTimeMillis() - 3600000, // 1시간 전
            endTime = System.currentTimeMillis(),
            stepCount = 12000,
            locations = emptyList(),
            totalDistance = 5000f,
            preWalkEmotion = EmotionType.JOYFUL,
            postWalkEmotion = EmotionType.JOYFUL,
            note = null,
            createdDate = java.time.ZonedDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
        )

        val mockGoal = Goal(
            targetStepCount = 10000,
            targetWalkCount = 5,
        )
        WalkingResultScreenContent(
            onNavigateToPrevious = {},
            onNavigateToHome = {},
            currentSession = mockSession,
            emotionPhotoUri = null,
            goal = mockGoal,
            syncedSessionsThisWeek = emptyList(),
            snapshotState = SnapshotState.Idle,
            onCaptureSnapshot = { false },
            onSyncSessionToServer = {},
            onDeleteNote = {},
            onUpdateNote = { _, _ -> },
            currentSessionLocalId = null,
        )
    }
}

