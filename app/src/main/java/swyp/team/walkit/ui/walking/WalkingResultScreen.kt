package swyp.team.walkit.ui.walking

import android.R.attr.fontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.components.KakaoMapView
import swyp.team.walkit.ui.components.ProgressIndicatorSize
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.ui.walking.utils.stringToEmotionType
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.presentation.viewmodel.KakaoMapViewModel
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.walking.components.GoalProgressCard
import swyp.team.walkit.ui.walking.components.PathThumbnail
import swyp.team.walkit.ui.walking.components.WalkingResultLoadingOverlay
import swyp.team.walkit.ui.walking.viewmodel.SnapshotState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.presentation.viewmodel.KakaoMapUiState
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.CtaButtonVariant
import swyp.team.walkit.ui.components.PreviousButton
import swyp.team.walkit.ui.components.SummaryUnit
import swyp.team.walkit.ui.components.WalkingSummaryCard
import swyp.team.walkit.ui.components.captureMapViewSnapshot
import swyp.team.walkit.ui.record.components.WalkingDiaryCard
import swyp.team.walkit.ui.record.components.WalkingStatsCard
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.walking.components.CoilBitmapImage
import swyp.team.walkit.ui.walking.components.ShareWalkingResultDialog
import swyp.team.walkit.ui.walking.components.SaveStatus
import swyp.team.walkit.utils.downloadImage
import swyp.team.walkit.ui.walking.components.WalkingProgressBar
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
    val boundsInRoot = coordinates.boundsInRoot()
    val window = activity.window

    Timber.d("📸 캡쳐 좌표 계산 시작")
    Timber.d("📸 boundsInWindow: $boundsInWindow")
    Timber.d("📸 boundsInRoot: $boundsInRoot")

    // 상태바 높이 계산 (Window 내 Content 영역 시작 위치)
    val statusBarHeight =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowInsets = window.decorView.rootWindowInsets
            windowInsets?.getInsets(android.view.WindowInsets.Type.statusBars())?.top ?: 0
        } else {
            @Suppress("DEPRECATION")
            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }

    // 네비게이션바 높이 계산
    val navigationBarHeight =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowInsets = window.decorView.rootWindowInsets
            windowInsets?.getInsets(android.view.WindowInsets.Type.navigationBars())?.bottom ?: 0
        } else {
            0
        }

    Timber.d("📸 시스템 UI 높이 - 상태바: $statusBarHeight, 네비게이션바: $navigationBarHeight")

    // boundsInRoot를 사용하여 스크롤 위치에 영향받지 않는 좌표 계산
    // boundsInRoot는 Root 레이아웃 기준 절대 좌표이므로 더 정확함
    val actualTop = boundsInRoot.top.toInt()
    val actualLeft = boundsInRoot.left.toInt()
    val actualRight = boundsInRoot.right.toInt()
    val actualBottom = boundsInRoot.bottom.toInt()

    Timber.d("📸 Root 기준 좌표 - left: $actualLeft, top: $actualTop, right: $actualRight, bottom: $actualBottom")
    Timber.d("📸 크기 - width: $width, height: $height")

    // Window 기준 좌표도 로깅 (비교용)
    Timber.d("📸 Window 기준 좌표 - left: ${boundsInWindow.left.toInt()}, top: ${boundsInWindow.top.toInt()}, right: ${boundsInWindow.right.toInt()}, bottom: ${boundsInWindow.bottom.toInt()}")

    // Bitmap 크기는 coordinates.size를 사용 (실제 뷰 크기)
    val bitmap = android.graphics.Bitmap.createBitmap(
        width,
        height,
        android.graphics.Bitmap.Config.ARGB_8888
    )

    Timber.d("📸 Bitmap 생성 - width: $width, height: $height")

    // PixelCopy는 Window 기준 좌표를 사용하므로 boundsInWindow 사용
    // 하지만 boundsInWindow가 잘못된 값을 반환할 수 있으므로 boundsInRoot와 비교하여 검증
    var windowTop = boundsInWindow.top.toInt()
    var windowLeft = boundsInWindow.left.toInt()
    var windowRight = boundsInWindow.right.toInt()
    var windowBottom = boundsInWindow.bottom.toInt()

    // Root와 Window 좌표 차이 계산 (좌표 계산 오류 확인)
    val scrollOffsetY = actualTop - windowTop
    val scrollOffsetX = actualLeft - windowLeft

    Timber.d("📸 좌표 차이 분석 - Y: $scrollOffsetY, X: $scrollOffsetX")

    // ⚠️ 핵심: boundsInWindow가 비정상적인 값을 반환하는 경우 감지
    // 1. 음수 좌표: 좌표 계산 오류 가능성
    // 2. 상태바 높이보다 작은 top 값: Edge-to-edge 모드에서 좌표 계산 오류 가능성
    // 3. boundsInRoot와 boundsInWindow의 차이가 비정상적으로 큰 경우: 좌표 계산 오류

    val windowWidth = window.decorView.width
    val windowHeight = window.decorView.height

    // 비정상적인 좌표 값 감지
    val hasNegativeCoordinates = windowTop < 0 || windowLeft < 0
    val hasInvalidTop = windowTop < -statusBarHeight // 상태바 높이보다 더 위로 나간 경우
    val hasInvalidBounds = windowRight > windowWidth || windowBottom > windowHeight
    val hasLargeOffset =
        kotlin.math.abs(scrollOffsetY) > windowHeight / 2 || kotlin.math.abs(scrollOffsetX) > windowWidth / 2

    // 좌표 계산 오류로 판단되는 경우
    val isCoordinateError = hasInvalidTop || hasLargeOffset

    if (hasNegativeCoordinates) {
        Timber.w("⚠️ 음수 좌표 감지 - top: $windowTop, left: $windowLeft")
        Timber.w("⚠️ boundsInRoot: top=$actualTop, left=$actualLeft")
        Timber.w("⚠️ 상태바 높이: $statusBarHeight")
    }

    if (isCoordinateError) {
        Timber.w("⚠️ 좌표 계산 오류로 판단됨!")
        Timber.w("⚠️ boundsInWindow가 잘못된 값을 반환했을 가능성이 높습니다")
        Timber.w("⚠️ boundsInRoot를 기준으로 좌표를 재계산합니다")

        // boundsInRoot를 기준으로 좌표 재계산
        // boundsInRoot는 Root 레이아웃 기준 절대 좌표이므로 더 정확함
        windowTop = actualTop.coerceAtLeast(0)
        windowLeft = actualLeft.coerceAtLeast(0)
        windowRight = actualRight.coerceAtMost(windowWidth)
        windowBottom = actualBottom.coerceAtMost(windowHeight)

        Timber.d("📸 boundsInRoot 기준으로 좌표 재계산 완료")
        Timber.d("📸 재계산된 좌표 - top: $windowTop, left: $windowLeft, right: $windowRight, bottom: $windowBottom")
    }

    // Window 기준 좌표 사용 (PixelCopy는 Window 기준)
    val rect = android.graphics.Rect(
        windowLeft,
        windowTop,
        windowRight,
        windowBottom
    )

    Timber.d("📸 최종 Rect (Window 기준) - ${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom}")
    Timber.d("📸 Window 크기 - width: $windowWidth, height: $windowHeight")

    // 좌표 유효성 검증
    if (rect.top < 0 || rect.left < 0 || rect.right > windowWidth || rect.bottom > windowHeight) {
        Timber.w("⚠️ 좌표가 Window 범위를 벗어남 - rect: $rect, window: ${windowWidth}x${windowHeight}")
        if (rect.top < 0) {
            Timber.w("⚠️ 상단 좌표가 음수입니다 (${rect.top}) - 상태바 높이: $statusBarHeight")
        }
    }

    // ⚠️ 중요: PixelCopy는 Window 내 보이는 부분만 캡쳐할 수 있습니다
    // 따라서 Window 범위 내로 좌표를 조정하되, Bitmap 크기는 뷰의 실제 크기로 유지합니다

    // Window 범위 내로 좌표 조정
    var adjustedRect = android.graphics.Rect(
        rect.left.coerceAtLeast(0),
        rect.top.coerceAtLeast(0),
        rect.right.coerceAtMost(windowWidth),
        rect.bottom.coerceAtMost(windowHeight)
    )

    // 조정된 Rect 크기 확인
    val adjustedWidth = adjustedRect.width()
    val adjustedHeight = adjustedRect.height()

    Timber.d("📸 조정된 Rect - $adjustedRect (크기: ${adjustedWidth}x${adjustedHeight})")
    Timber.d("📸 뷰 실제 크기 - width: $width, height: $height")

    // 조정된 Rect 크기가 뷰의 실제 크기와 다른 경우 확인
    if (adjustedWidth != width || adjustedHeight != height) {
        Timber.w("⚠️ 조정된 Rect 크기(${adjustedWidth}x${adjustedHeight})와 뷰 실제 크기(${width}x${height})가 불일치")
        Timber.w("⚠️ PixelCopy는 Window 내 보이는 부분만 캡쳐하므로, 일부가 잘릴 수 있습니다")
    }

    if (adjustedRect != rect) {
        Timber.d("📸 좌표 조정됨 - 원본: $rect, 조정: $adjustedRect")
    }

    val finalRect = adjustedRect

    // PixelCopy의 콜백을 suspend 함수로 변환
    // suspendCancellableCoroutine은 취소 가능한 코루틴으로, suspendCoroutine보다 권장됨
    return suspendCancellableCoroutine { continuation ->
        android.view.PixelCopy.request(
            window,
            finalRect,
            bitmap,
            { copyResult ->
                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    Timber.d("✅ 사진+경로 PixelCopy 스냅샷 생성 완료")
                    Timber.d("📸 Bitmap 크기 - width: ${bitmap.width}, height: ${bitmap.height}")
                    Timber.d("📸 Rect 크기 - width: ${finalRect.width()}, height: ${finalRect.height()}")

                    // Bitmap 크기와 Rect 크기가 일치하는지 확인
                    val finalBitmap = if (bitmap.width != finalRect.width() || bitmap.height != finalRect.height()) {
                        Timber.w("⚠️ Bitmap 크기(${bitmap.width}x${bitmap.height})와 Rect 크기(${finalRect.width()}x${finalRect.height()})가 불일치")
                        Timber.w("⚠️ Bitmap을 Rect 크기에 맞춰 크롭합니다")

                        // Bitmap을 Rect 크기에 맞춰 크롭 (중앙 기준)
                        val cropX = (bitmap.width - finalRect.width()) / 2
                        val cropY = (bitmap.height - finalRect.height()) / 2
                        val croppedBitmap = android.graphics.Bitmap.createBitmap(
                            bitmap,
                            cropX.coerceAtLeast(0),
                            cropY.coerceAtLeast(0),
                            finalRect.width().coerceAtMost(bitmap.width),
                            finalRect.height().coerceAtMost(bitmap.height)
                        )

                        bitmap.recycle() // 원본 bitmap 메모리 해제
                        // 하드웨어 비트맵을 소프트웨어 비트맵으로 복사하여 호환성 문제 해결
                        croppedBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false).also {
                            croppedBitmap.recycle()
                        }
                    } else {
                        // 하드웨어 비트맵을 소프트웨어 비트맵으로 복사하여 호환성 문제 해결
                        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false).also {
                            bitmap.recycle()
                        }
                    }

                    val savedPath = saveSnapshotToFile(context, finalBitmap)
                    Timber.d("✅ 스냅샷 파일 저장: $savedPath")
                    continuation.resume(savedPath) {}
                } else {
                    Timber.e("❌ 사진+경로 PixelCopy 실패: $copyResult")
                    Timber.e("❌ Rect: $finalRect, Bitmap: ${bitmap.width}x${bitmap.height}")
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
    } catch (t: Throwable) {
        Timber.e(t, "스냅샷 파일 저장 실패: ${t.message}")
        null
    }
}

/**
 * 산책 결과 화면 Screen
 * 상태 처리(Loading, Success, Error)를 담당합니다.
 */
@Composable
fun WalkingResultScreen(
    modifier: Modifier = Modifier,
    onNavigateToPrevious: () -> Unit,
    onNavigateToHome: () -> Unit,
    currentSession: WalkingSession?, // Flow에서 제공 (null이면 로딩/에러 상태)
    emotionPhotoUri: android.net.Uri?,
    weekWalkOrder: Int,
    goal: Goal?,
    syncedSessionsThisWeek: List<WalkingSession>,
    snapshotState: SnapshotState,
    onCaptureSnapshot: suspend (suspend () -> String?) -> Boolean,
    onSyncSessionToServer: () -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
) {
    when (currentSession) {
        null -> {
            // 로딩/에러 상태 (Flow에서 아직 데이터가 로드되지 않음)
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                    Text(
                        text = "세션 정보를 불러오는 중...",
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
                modifier = modifier,
                onNavigateToPrevious = onNavigateToPrevious,
                onNavigateToHome = onNavigateToHome,
                currentSession = currentSession,
                emotionPhotoUri = emotionPhotoUri,
                weekWalkOrder = weekWalkOrder,
                goal = goal,
                syncedSessionsThisWeek = syncedSessionsThisWeek,
                snapshotState = snapshotState,
                onCaptureSnapshot = onCaptureSnapshot,
                onSyncSessionToServer = onSyncSessionToServer,
                onUpdateNote = onUpdateNote,
                onDeleteNote = onDeleteNote,
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
    modifier: Modifier = Modifier,
    onNavigateToPrevious: () -> Unit,
    onNavigateToHome: () -> Unit,
    currentSession: WalkingSession,
    emotionPhotoUri: android.net.Uri?,
    goal: Goal?,
    weekWalkOrder: Int,
    syncedSessionsThisWeek: List<WalkingSession>,
    snapshotState: SnapshotState,
    onCaptureSnapshot: suspend (suspend () -> String?) -> Boolean,
    onSyncSessionToServer: () -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 이미지 저장 상태
    var saveStatus by remember { mutableStateOf(SaveStatus.IDLE) }
    val scope = rememberCoroutineScope()

    // 사진 + 경로 Box의 위치 정보 (스냅샷 생성용)
    var photoWithPathBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // 지도 MapView 참조 (스냅샷 생성용)
    var mapViewRef by remember { mutableStateOf<com.kakao.vectormap.MapView?>(null) }

    // MapView 렌더링 딜레이 상태 (초기 로딩 딜레이)
    var shouldShowMapView by remember { mutableStateOf(false) }

    // 화면 로딩 시 500ms 딜레이 후 MapView 표시
    LaunchedEffect(Unit) {
        delay(500)
        shouldShowMapView = true
        Timber.d("🗺️ MapView 렌더링 시작 (500ms 딜레이 완료)")
    }

    // MapView와 Box 위치 정보 준비 상태 추적
    val isMapViewReady by remember(mapViewRef) { derivedStateOf { mapViewRef != null } }
    val isBoxCoordinatesReady by remember(photoWithPathBoxCoordinates) { derivedStateOf { photoWithPathBoxCoordinates != null } }
    // 사진이 있으면 Box 좌표만 필요, 없으면 MapView 필요
    val isSnapshotReady by remember {
        derivedStateOf {
            if (emotionPhotoUri != null) {
                isBoxCoordinatesReady
            } else {
                isMapViewReady
            }
        }
    }

    // 고유 팝업 표시 여부
    var showShareDialog by remember { mutableStateOf(false) }

    // 캡쳐된 스냅샷 경로 (공유 다이얼로그에서 사용)
    var capturedSnapshotPath by remember { mutableStateOf<String?>(null) }

    var isEditing by remember { mutableStateOf(false) }
    var editedNote by remember { mutableStateOf(currentSession.note ?: "") }

    LaunchedEffect(currentSession.note) {
        if (!isEditing) { // 사용자가 편집 중이 아닐 때만 업데이트
            editedNote = currentSession.note ?: ""
        }
    }

    // LazyColumn 스크롤 상태 관리
    val lazyListState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize().background(SemanticColor.backgroundWhiteSecondary)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
        ) {
            // 진행 바 (1번째 칸 채워짐)
            item {
                WalkingProgressBar(
                    currentStep = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
            }

            item {
                Column {
                    Text(
                        text = "오늘도 산책 완료!",
                        // heading S/semibold
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("이번 주 ")

                                withStyle(
                                    style = SpanStyle(color = SemanticColor.stateAquaBluePrimary)
                                ) {
                                    append("${weekWalkOrder}번째")
                                }

                                append(" 산책을 완료했어요.")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SemanticColor.textBorderSecondary, // 기본 색
                        )

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (capturedSnapshotPath == null) {
                                        Timber.d("공유하기: 스냅샷이 없어 생성 시작")
                                        var snapshotPath: String? = null
                                        val success = onCaptureSnapshot {
                                            try {
                                                snapshotPath = if (emotionPhotoUri != null) {
                                                    capturePhotoWithPathSnapshot(
                                                        photoWithPathBoxCoordinates,
                                                        context
                                                    )
                                                } else {
                                                    if (mapViewRef != null) {
                                                        captureMapViewSnapshot(
                                                            mapViewRef!!,
                                                            context
                                                        )
                                                    } else {
                                                        Timber.w("MapView 참조가 없습니다 - 스냅샷 생성 실패")
                                                        null
                                                    }
                                                }
                                                snapshotPath
                                            } catch (t: Throwable) {
                                                Timber.e(t, "공유용 스냅샷 생성 실패")
                                                null
                                            }
                                        }

                                        if (success && snapshotPath != null) {
                                            capturedSnapshotPath = snapshotPath
                                        } else {
                                            Timber.w("공유용 스냅샷 생성 실패 - 다이얼로그 표시 안 함")
                                            return@launch
                                        }
                                    }

                                    showShareDialog = true
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_external),
                                tint = SemanticColor.iconGrey,
                                contentDescription = "external",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .aspectRatio(1f)
                        .onGloballyPositioned { coordinates ->
                            // 사진이 있는 경우에만 위치 정보 저장 (스냅샷 생성용)
                            if (emotionPhotoUri != null) {
                                photoWithPathBoxCoordinates = coordinates
                            }
                        }
                        .background(
                            Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(12.dp)
                        ) // Card 대신 배경 + 모서리 둥글게
                ) {
                    if (emotionPhotoUri != null) {
                        // 케이스 1: 사진이 있는 경우
                        val bitmap = remember(emotionPhotoUri) {
                            try {
                                val mimeType = context.contentResolver.getType(emotionPhotoUri!!)
                                val isVideo = mimeType?.startsWith("video/") == true

                                if (isVideo) {
                                    Timber.w("영상 파일이 감정 기록에 설정됨 - 이미지 표시 불가: $mimeType")
                                    null
                                } else {
                                    val inputStream =
                                        context.contentResolver.openInputStream(emotionPhotoUri)
                                    android.graphics.BitmapFactory.decodeStream(inputStream)
                                }
                            } catch (t: Throwable) {
                                Timber.e(t, "이미지 변환 실패")
                                null
                            }
                        }

                        emotionPhotoUri?.let { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data(uri)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "사진",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // 다이얼로그 스타일 오버레이 + 경로 표시
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            PathThumbnail(
                                locations = currentSession.locations,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                pathColor = Color.White,
                                startColor = Color.White,
                                endColor = Color.White,
                            )
                        }
                    } else {
                        // 케이스 2: 사진이 없는 경우 - MapView 직접 표시
                        KakaoMapView(
                            locations = currentSession.locations,
                            modifier = Modifier.fillMaxSize(),
                            viewModel = mapViewModel,
                            onMapViewReady = { mapView ->
                                mapViewRef = mapView
                            },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
            }

            item {
                WalkingStatsCard(
                    sessions = listOf(currentSession),
                    stepsLabel = "걸음 수",
                    durationLabel = "산책 시간"
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(16.dp))
            }
            item {
                WalkingDiaryCard(
                    session = currentSession,
                    note = editedNote,
                    isEditMode = isEditing,
                    setEditing = { isEditing = it },
                    onNoteChange = { editedNote = it },
                    onDeleteClick = {
                        onDeleteNote(currentSession.id)
                        editedNote = "" // 삭제 후 UI 상태 초기화
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreviousButton(
                            onClick = onNavigateToPrevious,
                            enabled = snapshotState is SnapshotState.Idle ||
                                    snapshotState is SnapshotState.Error,
                        )

                        CtaButton(
                            onClick = {
                                coroutineScope.launch {
                                    // 1. 스크롤을 맨 위로 올림 (뷰가 Window 내에 완전히 보이도록 보장)
                                    Timber.d("📸 저장하기 클릭 - 스크롤을 맨 위로 이동 시작")
                                    try {
                                        lazyListState.animateScrollToItem(0)

                                        // 스크롤 애니메이션 완료 대기
                                        // 스크롤이 진행 중이면 완료될 때까지 대기
                                        var retryCount = 0
                                        while (lazyListState.isScrollInProgress && retryCount < 50) {
                                            kotlinx.coroutines.delay(50)
                                            retryCount++
                                        }

                                        // 추가 안정화 시간 (레이아웃 재계산 대기)
                                        kotlinx.coroutines.delay(100)

                                        Timber.d("📸 스크롤 완료 - 현재 스크롤 위치: ${lazyListState.firstVisibleItemIndex}")
                                        Timber.d("📸 캡쳐 시작")
                                    } catch (e: Exception) {
                                        Timber.e(e, "스크롤 이동 실패")
                                        // 스크롤 실패해도 캡쳐는 진행
                                    }

                                    // 2. 수정된 노트 저장 (editedNote가 원본과 다른 경우에만)
                                    if (editedNote != currentSession.note.orEmpty()) {
                                        onUpdateNote(currentSession.id, editedNote)
                                    }

                                    // 3. 스냅샷 생성 및 저장
                                    var snapshotPath: String? = null
                                    val success = onCaptureSnapshot {
                                        try {
                                            snapshotPath = if (emotionPhotoUri != null) {
                                                // 케이스 1: 사진 + 경로 스냅샷 (맵뷰 로딩 없음)
                                                capturePhotoWithPathSnapshot(
                                                    photoWithPathBoxCoordinates,
                                                    context
                                                )
                                            } else {
                                                // 케이스 2: 지도 + 경로 스냅샷
                                                // MapView를 PixelCopy로 캡처
                                                if (mapViewRef != null) {
                                                    captureMapViewSnapshot(
                                                        mapViewRef!!,
                                                        context
                                                    )
                                                } else {
                                                    Timber.w("MapView 참조가 없습니다 - 스냅샷 생성 실패")
                                                    null
                                                }
                                            }
                                            snapshotPath
                                        } catch (t: Throwable) {
                                            Timber.e(t, "스냅샷 생성 실패")
                                            null
                                        }
                                    }

                                    // 4. 스냅샷 저장 완료 후 서버 동기화 시작
                                    if (success && snapshotPath != null) {
                                        capturedSnapshotPath = snapshotPath
                                        onSyncSessionToServer()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isSnapshotReady && (snapshotState is SnapshotState.Idle ||
                                    snapshotState is SnapshotState.Error),
                            text = "저장하기"
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        // 저장 중 오버레이
        if (snapshotState is SnapshotState.Capturing ||
            snapshotState is SnapshotState.Saving ||
            snapshotState is SnapshotState.Syncing
        ) {
            WalkingResultLoadingOverlay()
        }

        if (showShareDialog) {
            ShareWalkingResultDialog(
                stepCount = currentSession.stepCount.toString(),
                duration = currentSession.duration,
                onDismiss = { showShareDialog = false },
                onPrev = { showShareDialog = false },
                preWalkEmotion = currentSession.preWalkEmotion,
                postWalkEmotion = currentSession.postWalkEmotion,
                saveStatus = saveStatus,
                onSave = {
                    scope.launch {
                        try {
                            saveStatus = SaveStatus.LOADING
                            downloadImage(
                                context = context,
                                path = capturedSnapshotPath ?: "",
                                fileName = "walking_result_${currentSession.id}.png"
                            )
                            saveStatus = SaveStatus.SUCCESS
                            Timber.d("이미지 저장 성공")
                        } catch (t: Throwable) {
                            saveStatus = SaveStatus.FAILURE
                            Timber.e(t, "이미지 저장 실패")
                        }
                    }
                },
                sessionThumbNailUri = capturedSnapshotPath ?: ""
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
            preWalkEmotion = "JOYFUL",
            postWalkEmotion = "JOYFUL",
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
            onUpdateNote = { _, _ -> },
            weekWalkOrder = 2,
            onDeleteNote = {},
        )
    }
}

