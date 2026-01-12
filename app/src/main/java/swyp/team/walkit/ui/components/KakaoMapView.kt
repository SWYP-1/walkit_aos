package swyp.team.walkit.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.LocalContext
import com.kakao.vectormap.graphics.gl.GLSurfaceView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import com.kakao.vectormap.LatLngBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.GestureType
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.presentation.viewmodel.CameraSettings
import swyp.team.walkit.presentation.viewmodel.KakaoMapViewModel
import swyp.team.walkit.presentation.viewmodel.KakaoMapUiState
import swyp.team.walkit.presentation.viewmodel.MapRenderState
import swyp.team.walkit.ui.theme.SemanticColor
import timber.log.Timber

/**
 * 상수 정의
 */
private object MapSnapshotConstants {
    const val ROUTE_LINE_WIDTH = 16f
    const val ROUTE_LINE_COLOR = "#4285F4"
    const val RENDER_FRAMES_TO_WAIT = 5 // GPU 렌더링 완료를 위해 대기할 프레임 수 (타일 로딩 보장)
    const val TILE_LOADING_EXTRA_DELAY_MS = 300L // 마지막 프레임 후 추가 타일 로딩 대기 시간
}

/**
 * KakaoMap을 Compose에서 사용하기 위한 컴포저블
 * UI 렌더링과 MapView 제어만 담당하며, 비즈니스 로직은 ViewModel에서 처리합니다.
 *
 * @param locations 경로를 표시할 위치 좌표 리스트
 * @param modifier Modifier
 * @param viewModel KakaoMapViewModel (옵션, 없으면 자동 생성)
 * @param onMapViewReady MapView가 준비되었을 때 호출되는 콜백 (스냅샷 생성용)
 */
@Composable
fun KakaoMapView(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    viewModel: KakaoMapViewModel = hiltViewModel(),
    onMapViewReady: ((MapView?) -> Unit)? = null,
    updateTrigger: Int = 0, // 강제 업데이트 트리거
    latLngBoundsPaddingPx: Int = 64, // LatLngBounds 패딩 (픽셀)
) {
    val context = LocalContext.current
    val localDensity = LocalDensity.current

    // ViewModel 상태 구독
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()

    // MapView 관련 상태 (UI 제어용)
    var kakaoMapInstance by remember {
        mutableStateOf<KakaoMap?>(null)
    }
    var mapViewRef by remember {
        mutableStateOf<MapView?>(null)
    }
    var mapStarted by remember {
        mutableStateOf(false)
    }

    // MapView 크기 상태
    var mapViewSize by remember {
        mutableStateOf<IntSize?>(null)
    }

    // 화면 크기 측정 및 ViewModel에 전달
    LaunchedEffect(mapViewSize) {
        mapViewSize?.let { size ->
            if (size.width > 0 && size.height > 0) {
                viewModel.setMapViewSize(size.width, size.height,localDensity)
                Timber.d("MapView 크기 측정 완료: ${size.width}x${size.height}")
            }
        }
    }

    // ViewModel에 locations 전달 (크기가 측정되면 자동으로 재계산됨)
    LaunchedEffect(locations) {
        viewModel.setLocations(locations,localDensity)
    }

    // 렌더링 상태에 따라 작업 수행
    LaunchedEffect(renderState) {
        val kakaoMap = kakaoMapInstance
        val mapView = mapViewRef
        if (kakaoMap == null || mapView == null) return@LaunchedEffect

        when (renderState) {
            is MapRenderState.DrawingPath -> {
                val currentUiState = uiState
                if (currentUiState is KakaoMapUiState.Ready && currentUiState.shouldDrawPath) {
                    Timber.d("경로 그리기 시작")
//                    drawPath(kakaoMap, currentUiState.locations, viewModel, mapView)
                } else {
                    // 경로가 없으면 바로 Complete 상태로
                    viewModel.onPathDrawComplete()
                }
            }

            else -> {}
        }
    }
    val strokePx = with(localDensity) { 4.dp.toPx() }
    val latLngBoundsPaddingPxFloat = latLngBoundsPaddingPx.toFloat()

    // UI 상태 변경 시 지도 업데이트
    LaunchedEffect(uiState, kakaoMapInstance, mapViewRef, updateTrigger) {
        val map = kakaoMapInstance
        val mapView = mapViewRef
        if (map != null && mapView != null && mapStarted) {
            updateMapFromState(map, mapView, uiState, viewModel, context, latLngBoundsPaddingPxFloat, strokePx)
        }
    }

    // 로딩 상태 확인: Complete 상태가 아니면 로딩 중
    val isLoading = renderState != MapRenderState.Complete


    // 지도뷰와 로딩 인디케이터를 겹쳐서 표시
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                mapViewSize = size
            }
    ) {
        // MapView는 항상 VISIBLE로 표시
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    visibility = View.VISIBLE
                    mapViewRef = this
                    // MapView 참조를 외부에 전달 (스냅샷 생성용)
                    onMapViewReady?.invoke(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // MapView는 항상 VISIBLE로 유지
                mapView.visibility = View.VISIBLE

                if (!mapStarted) {
                    mapStarted = true
                    initializeMapView(mapView, viewModel, uiState, context, strokePx, latLngBoundsPaddingPxFloat) { kakaoMap ->
                        kakaoMapInstance = kakaoMap
                    }
                }
            },
        )

        // 로딩 중일 때 투명한 회색 배경과 프로그레스 바 표시
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)), // 투명한 회색 배경 (50% 투명도)
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator(
                    size = ProgressIndicatorSize.Medium,
                )
            }
        }
    }
}

/**
 * MapView 초기화 및 리스너 설정
 */
private fun initializeMapView(
    mapView: MapView,
    viewModel: KakaoMapViewModel,
    uiState: KakaoMapUiState,
    context: Context,
    strokePx: Float,
    latLngBoundsPaddingPx : Float,
    onMapReady: (KakaoMap) -> Unit,
) {
    mapView.start(
        object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Timber.d("KakaoMap destroyed")
            }

            override fun onMapError(error: Exception) {
                Timber.e(error, "KakaoMap error")
            }
        },
        object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                Timber.d("KakaoMap ready")
                setupCameraListener(kakaoMap, viewModel)
                onMapReady(kakaoMap)
                updateMapFromState(kakaoMap, mapView, uiState, viewModel, context, latLngBoundsPaddingPx,strokePx)
            }
        },
    )
}

/**
 * 카메라 이동 완료 리스너 설정
 */
private fun setupCameraListener(
    kakaoMap: KakaoMap,
    viewModel: KakaoMapViewModel,
) {
    kakaoMap.setOnCameraMoveEndListener(null)
    kakaoMap.setOnCameraMoveEndListener { _: KakaoMap, _: CameraPosition, gestureType: GestureType ->
        if (gestureType == GestureType.Unknown &&
            viewModel.renderState.value == MapRenderState.MovingCamera
        ) {
            Timber.d("카메라 이동 완료 (프로그래밍 방식)")
            viewModel.onCameraMoveComplete()
        }
    }
}

/**
 * ViewModel 상태에 따라 지도 업데이트
 */
private fun updateMapFromState(
    kakaoMap: KakaoMap,
    mapView: MapView,
    uiState: KakaoMapUiState,
    viewModel: KakaoMapViewModel,
    context: Context,
    latLngBoundsPaddingPx : Float,
    strokePx: Float,  // ← 파라미터로 받
) {
    when (uiState) {
        is KakaoMapUiState.Ready -> {
            // 이미 경로 그리기 중이면 중복 호출 방지 (카메라 이동은 허용)
            val currentRenderState = viewModel.renderState.value
            if (currentRenderState == MapRenderState.DrawingPath) {
                Timber.d("경로 그리기 진행 중: $currentRenderState - 업데이트 스킵")
                return
            }

            try {
                // 카메라 이동 시작 (MovingCamera 상태에서도 허용)
                viewModel.startCameraMove()
                moveCameraToPathWithLatLngBounds(kakaoMap, uiState.locations, latLngBoundsPaddingPx.toInt())

                // 경로 그리기 또는 제거
                if (uiState.shouldDrawPath && currentRenderState == MapRenderState.Idle) {
                    drawPath(kakaoMap, uiState.locations, viewModel, context, mapView, strokePx)
                } else if (!uiState.shouldDrawPath) {
                    // 경로 제거
                    clearPath(kakaoMap)
                }
            } catch (t: Throwable) {
                Timber.e(t, "지도 업데이트 실패")
            }
        }

        is KakaoMapUiState.Error -> {
            Timber.e("지도 오류: ${uiState.message}")
        }

        is KakaoMapUiState.Initial -> {
            // 초기 상태 - 아무 작업도 하지 않음
        }
    }
}

/**
 * MapView를 PixelCopy로 캡처하는 suspend 함수
 *
 * @param mapView 캡처할 MapView
 * @param context Context
 * @return 스냅샷 파일 경로 (실패 시 null)
 */
suspend fun captureMapViewSnapshot(
    mapView: MapView,
    context: Context,
): String? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    captureViewSnapshot(mapView, context) { path ->
        continuation.resume(path) {}
    }

    continuation.invokeOnCancellation {
        Timber.d("MapView 스냅샷 생성 취소됨")
    }
}

/**
 * 썸네일용 MapView PixelCopy 캡처 (DailyRecordScreen용)
 * MapView의 실제 화면 좌표를 고려해서 PixelCopy 수행
 */
suspend fun captureMapViewSnapshotForThumbnail(
    context: android.content.Context,
    mapView: com.kakao.vectormap.MapView,
): String? {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
        Timber.w("PixelCopy는 Android 8.0 이상에서만 사용 가능")
        return null
    }

    val activity = context as? android.app.Activity
    if (activity == null) {
        Timber.w("Activity를 찾을 수 없습니다")
        return null
    }

    // MapView의 크기 가져오기
    val width = mapView.width
    val height = mapView.height

    if (width <= 0 || height <= 0) {
        Timber.w("MapView 크기가 0입니다: ${width}x${height}")
        return null
    }

    val bitmap = android.graphics.Bitmap.createBitmap(
        width,
        height,
        android.graphics.Bitmap.Config.ARGB_8888
    )

    // MapView의 bounds를 window 좌표로 변환하기 위해 뷰의 위치 정보 필요
    val location = IntArray(2)
    mapView.getLocationInWindow(location)
    val left = location[0]
    val top = location[1]

    val rect = android.graphics.Rect(left, top, left + width, top + height)
    val window = activity.window

    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        android.view.PixelCopy.request(
            window,
            rect,
            bitmap,
            { copyResult ->
                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    Timber.d("썸네일 MapView PixelCopy 스냅샷 생성 완료: ${bitmap.width}x${bitmap.height}")
                    val savedPath = saveSnapshotToFile(context, bitmap)
                    Timber.d("썸네일 스냅샷 파일 저장: $savedPath")
                    continuation.resume(savedPath) {}
                } else {
                    Timber.e("썸네일 MapView PixelCopy 실패: $copyResult")
                    continuation.resume(null) {}
                }
            },
            android.os.Handler(android.os.Looper.getMainLooper())
        )

        continuation.invokeOnCancellation {
            Timber.d("썸네일 PixelCopy 요청 취소됨")
        }
    }
}

/**
 * 공통 스냅샷 유틸리티: View를 PixelCopy로 캡처하는 범용 함수
 *
 * @param view 캡처할 View
 * @param context Context
 * @param onComplete 스냅샷 생성 완료 시 파일 경로를 반환하는 콜백
 */
fun captureViewSnapshot(
    view: View,
    context: Context,
    onComplete: (String?) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        Timber.e("PixelCopy는 Android 8.0 이상에서만 사용 가능")
        onComplete(null)
        return
    }

    try {
        if (view.width == 0 || view.height == 0) {
            Timber.w("View 크기가 0입니다: ${view.width}x${view.height}")
            onComplete(null)
            return
        }

        view.visibility = View.VISIBLE

        // View가 GLSurfaceView인 경우
        val glSurfaceView = findGLSurfaceView(view)
        if (glSurfaceView != null) {
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            waitForFramesToRender(glSurfaceView, "ViewSnapshot") {
                performPixelCopy(glSurfaceView, context, onComplete)
            }
        } else {
            // 일반 View인 경우 Window를 통해 PixelCopy 수행
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                val bitmap = Bitmap.createBitmap(
                    view.width,
                    view.height,
                    Bitmap.Config.ARGB_8888
                )

                val location = IntArray(2)
                view.getLocationInWindow(location)

                PixelCopy.request(
                    window,
                    android.graphics.Rect(
                        location[0],
                        location[1],
                        location[0] + view.width,
                        location[1] + view.height
                    ),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            Timber.d("View PixelCopy 스냅샷 생성 완료: ${bitmap.width}x${bitmap.height}")
                            val savedPath = saveSnapshotToFile(context, bitmap)
                            Timber.d("View PixelCopy 스냅샷 파일 저장: $savedPath")
                            onComplete(savedPath)
                        } else {
                            Timber.e("View PixelCopy 실패: $copyResult")
                            onComplete(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                Timber.e("Activity Window를 찾을 수 없습니다")
                onComplete(null)
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "View 스냅샷 생성 실패: ${t.message}")
        onComplete(null)
    }
}

/**
 * PixelCopy API를 사용한 스냅샷 캡처 (Android 8.0+)
 */
private fun captureUsingPixelCopy(
    glSurfaceView: GLSurfaceView,
    mapView: MapView,
    context: Context,
    onComplete: (String?) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        Timber.e("PixelCopy는 Android 8.0 이상에서만 사용 가능")
        onComplete(null)
        return
    }

    try {
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        waitForFramesToRender(glSurfaceView, "PixelCopy") {
            performPixelCopy(glSurfaceView, context, onComplete)
        }
    } catch (t: Throwable) {
        Timber.e(t, "PixelCopy 준비 실패: ${t.message}")
        onComplete(null)
    }
}

/**
 * 실제 PixelCopy 수행
 */
private fun performPixelCopy(
    glSurfaceView: GLSurfaceView,
    context: Context,
    onComplete: (String?) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        onComplete(null)
        return
    }

    try {
        val bitmap = Bitmap.createBitmap(
            glSurfaceView.width,
            glSurfaceView.height,
            Bitmap.Config.ARGB_8888
        )

        PixelCopy.request(
            glSurfaceView,
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    Timber.d("PixelCopy 스냅샷 생성 완료: ${bitmap.width}x${bitmap.height}")
                    val savedPath = saveSnapshotToFile(context, bitmap)
                    Timber.d("PixelCopy 스냅샷 파일 저장: $savedPath")
                    onComplete(savedPath)
                } else {
                    Timber.e("PixelCopy 실패: $copyResult")
                    onComplete(null)
                }
            },
            Handler(Looper.getMainLooper())
        )
    } catch (t: Throwable) {
        Timber.e(t, "PixelCopy 실행 실패: ${t.message}")
        onComplete(null)
    }
}

/**
 * MapView에서 GLSurfaceView 찾기
 */
private fun findGLSurfaceView(view: android.view.View): GLSurfaceView? {
    if (view is GLSurfaceView) {
        return view
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            val glSurfaceView = findGLSurfaceView(child)
            if (glSurfaceView != null) {
                return glSurfaceView
            }
        }
    }
    return null
}


/**
 * 스냅샷을 파일로 저장
 */
private fun saveSnapshotToFile(
    context: Context,
    bitmap: Bitmap,
): String? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "map_snapshot_${timestamp}.png"

        val fileDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(fileDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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
 * 기존 경로 제거
 */
private fun clearPath(kakaoMap: KakaoMap) {
    try {
        val routeLineManager = kakaoMap.routeLineManager ?: return

        // 모든 라인 레이어 제거
        routeLineManager.layer.removeAll()
        Timber.d("기존 경로 라인 모두 제거됨")
    } catch (t: Throwable) {
        Timber.e(t, "경로 제거 실패")
    }
}

/**
 * 경로 그리기
 */
private fun drawPath(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
    viewModel: KakaoMapViewModel,
    context: Context,
    mapView: MapView,
    strokePx: Float
) {
    if (locations.isEmpty()) {
        Timber.d("경로 포인트가 없습니다")
        viewModel.onPathDrawComplete()
        return
    }

    if (locations.size < 2) {
        Timber.d("경로 포인트가 1개뿐입니다: ${locations.size}개 - 마커만 표시")
        viewModel.onPathDrawComplete()
        return
    }

    try {
        val routeLineManager = kakaoMap.routeLineManager
            ?: run {
                Timber.e("RouteLineManager를 가져올 수 없습니다")
                viewModel.onPathDrawComplete()
                return
            }

        // ✅ 새로운 경로를 그리기 전에 기존 경로 모두 제거
        routeLineManager.layer.removeAll()
        Timber.d("새 경로 그리기 전 기존 경로 모두 제거")

        val (outlineOptions, mainOptions) = createRouteLineOptions(locations, context, strokePx)

        // 윤곽선 먼저 추가
        routeLineManager.layer.addRouteLine(outlineOptions) { _, _ ->
            Timber.d("윤곽선 RouteLine 생성 완료")
            // 본선 추가
            routeLineManager.layer.addRouteLine(mainOptions) { _, _ ->
                Timber.d("본선 RouteLine 생성 완료")
                // 경로 그리기 완료 - 바로 Complete 상태로 변경 (스냅샷 생성 없음)
                viewModel.onPathDrawComplete()
            }
        }
        Timber.d("RouteLine 추가 요청 완료 (콜백 등록됨)")

    } catch (t: Throwable) {
        Timber.e(t, "RouteLine 그리기 실패: ${t.message}")
        t.printStackTrace()
        viewModel.onPathDrawComplete()
    }
}

/**
 * 여러 프레임 렌더링 대기 후 콜백 실행 (공통 로직)
 */
private fun waitForFramesToRender(
    glSurfaceView: GLSurfaceView,
    logPrefix: String,
    onComplete: () -> Unit,
) {
    var framesRendered = 0

    fun renderNextFrame() {
        glSurfaceView.requestRender()
        glSurfaceView.queueEvent {
            Handler(Looper.getMainLooper()).post {
                framesRendered++
                Timber.d("$logPrefix 프레임 렌더링 완료: $framesRendered/${MapSnapshotConstants.RENDER_FRAMES_TO_WAIT}")

                if (framesRendered < MapSnapshotConstants.RENDER_FRAMES_TO_WAIT) {
                    renderNextFrame()
                } else {
                    Timber.d("$logPrefix 모든 프레임 렌더링 완료 - 타일 로딩 추가 대기 중 (${MapSnapshotConstants.TILE_LOADING_EXTRA_DELAY_MS}ms)")
                    Handler(Looper.getMainLooper()).postDelayed({
                        onComplete()
                    }, MapSnapshotConstants.TILE_LOADING_EXTRA_DELAY_MS)
                }
            }
        }
    }

    renderNextFrame()
}


/**
 * RouteLine 옵션 생성 - 윤곽선과 본선을 별도의 라인으로 생성
 */
private fun createRouteLineOptions(
    locations: List<LocationPoint>,
    context: Context,
    strokePx: Float
): Pair<RouteLineOptions, RouteLineOptions> {
    val latLngList = locations.map { location ->
        LatLng.from(location.latitude, location.longitude)
    }

    // 윤곽선 옵션 (#1C1C1E, 더 굵은 선)
    val outlineStyle = RouteLineStyle.from(
        strokePx, // 윤곽선은 더 굵게
        Color(0xFF1C1C1E).toArgb()
    )
    val outlineStyles = RouteLineStyles.from(outlineStyle)
    val outlineStylesSet = RouteLineStylesSet.from(outlineStyles)
    val outlineSegment = RouteLineSegment.from(latLngList)
        .setStyles(outlineStylesSet.getStyles(0))
    val outlineOptions = RouteLineOptions.from(outlineSegment)
        .setStylesSet(outlineStylesSet)

    // 본선 옵션 (흰색, 기존 너비)
    val mainLineStyle = RouteLineStyle.from(
        strokePx,
        SemanticColor.stateAquaBluePrimary.toArgb()
    )
    val mainStyles = RouteLineStyles.from(mainLineStyle)
    val mainStylesSet = RouteLineStylesSet.from(mainStyles)
    val mainSegment = RouteLineSegment.from(latLngList)
        .setStyles(mainStylesSet.getStyles(0))
    val mainOptions = RouteLineOptions.from(mainSegment)
        .setStylesSet(mainStylesSet)

    return Pair(outlineOptions, mainOptions)
}

/**
 * 카메라 이동
 */
/**
 * LatLngBounds를 사용해서 경로에 맞춰 카메라 이동 (테두리에 걸리지 않음 보장)
 */
private fun moveCameraToPathWithLatLngBounds(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
    paddingPx: Int = 64,
) {
    try {
        if (locations.isEmpty()) {
            Timber.d("경로 포인트가 없어 카메라 이동 스킵")
            return
        }

        // LatLngBounds 생성
        val boundsBuilder = LatLngBounds.Builder()
        locations.forEach { location ->
            boundsBuilder.include(LatLng.from(location.latitude, location.longitude))
        }
        val bounds = boundsBuilder.build()

        // LatLngBounds를 사용한 카메라 업데이트 (패딩 적용)
        val cameraUpdate = CameraUpdateFactory.fitMapPoints(bounds, paddingPx)

        kakaoMap.moveCamera(cameraUpdate)
        Timber.d("LatLngBounds 카메라 이동: 포인트 ${locations.size}개, 패딩 ${paddingPx}px")

    } catch (t: Throwable) {
        Timber.e(t, "LatLngBounds 카메라 이동 실패: ${t.message}")
        // 실패 시 기존 방식으로 폴백
        Timber.d("기존 방식으로 폴백 시도")
        // TODO: cameraSettings를 받아서 폴백 구현 필요
    }
}

/**
 * 기존 방식의 카메라 이동 (LatLngBounds 실패 시 폴백용)
 */
private fun moveCameraToPath(
    kakaoMap: KakaoMap,
    cameraSettings: CameraSettings,
) {
    try {
        val centerPosition = LatLng.from(cameraSettings.centerLat, cameraSettings.centerLon)
        val cameraUpdate =
            CameraUpdateFactory.newCenterPosition(centerPosition, cameraSettings.zoomLevel)

        kakaoMap.moveCamera(cameraUpdate)
        Timber.d("카메라 이동 요청: 중심 (${cameraSettings.centerLat}, ${cameraSettings.centerLon}), 줌 레벨: ${cameraSettings.zoomLevel}")

        // 경계 정보가 있으면 로그 출력
        if (cameraSettings.minLat != null && cameraSettings.maxLat != null &&
            cameraSettings.minLon != null && cameraSettings.maxLon != null
        ) {
            Timber.d(
                "경계 정보: minLat=${cameraSettings.minLat}, maxLat=${cameraSettings.maxLat}, " +
                        "minLon=${cameraSettings.minLon}, maxLon=${cameraSettings.maxLon}"
            )
        }
    } catch (t: Throwable) {
        Timber.e(t, "카메라 이동 실패: ${t.message}")
    }
}