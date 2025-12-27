package team.swyp.sdu.ui.components

import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.EntryPointAccessors
import team.swyp.sdu.di.LocationManagerEntryPoint
import team.swyp.sdu.domain.service.LocationManager
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polyline
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.PolylineStyle
import com.kakao.vectormap.shape.PolylineStyles
import com.kakao.vectormap.shape.ShapeLayer
import com.kakao.vectormap.shape.ShapeManager
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import kotlinx.coroutines.launch
import timber.log.Timber

// 썸네일 관련 로그 필터링을 위한 TAG
private const val TAG_THUMBNAIL = "MapThumbnail"

/**
 * 카카오맵 스냅샷을 사용한 경로 썸네일 컴포넌트
 *
 * 실제 지도뷰를 사용하여 썸네일을 생성합니다.
 * 건물, 도로 등 실제 지도 정보가 포함됩니다.
 */
@Composable
fun RouteThumbnailMap(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
    stableKey: String? = null, // 컴포저블 재생성 방지를 위한 안정적인 키
    snapshotBitmap: androidx.compose.runtime.MutableState<Bitmap?>? = null, // 부모에서 관리하는 스냅샷 비트맵 (옵션)
    locationManager: LocationManager? = null, // LocationManager (선택사항, 없으면 내부에서 생성)
) {
    val context = LocalContext.current
    
    // LocationManager 주입 (없으면 내부에서 생성)
    val locationManagerInstance = locationManager ?: remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LocationManagerEntryPoint::class.java
        ).locationManager()
    }

    // 컴포저블 재생성 추적
    androidx.compose.runtime.SideEffect {
        Timber.tag(TAG_THUMBNAIL).d("컴포저블 실행 - stableKey=$stableKey, locations.size=${locations.size}")
    }

    // 전체 경로의 중앙값(중심점)을 계산하여 키로 사용
    // 썸네일은 전체 경로의 중앙값을 사용하므로, 중앙값이 같으면 같은 썸네일로 간주
    val locationsKey =
        remember(locations) {
            if (locations.isNotEmpty()) {
                // 전체 경로의 중앙값 계산
                var minLat = locations[0].latitude
                var maxLat = locations[0].latitude
                var minLon = locations[0].longitude
                var maxLon = locations[0].longitude

                locations.forEach { location ->
                    minLat = minOf(minLat, location.latitude)
                    maxLat = maxOf(maxLat, location.latitude)
                    minLon = minOf(minLon, location.longitude)
                    maxLon = maxOf(maxLon, location.longitude)
                }

                val centerLat = (minLat + maxLat) / 2
                val centerLon = (minLon + maxLon) / 2

                // 중앙값을 키로 사용 (소수점 6자리까지 반올림하여 안정적인 키 생성)
                "${String.format("%.6f", centerLat)},${String.format("%.6f", centerLon)}"
            } else {
                "empty"
            }
        }

    // stableKey만 사용하여 remember가 재생성되지 않도록 함
    // stableKey가 없으면 컴포저블 재생성 시 상태가 초기화될 수 있음
    val rememberKey = stableKey ?: "default"

    // 스냅샷 비트맵: 부모에서 전달받거나 내부에서 생성
    // 부모에서 전달받으면 재생성 시에도 유지됨
    val snapshotBitmapState =
        if (snapshotBitmap != null) {
            // 부모에서 전달받은 경우: 재생성 시에도 유지됨
            Timber.tag(TAG_THUMBNAIL).d("부모에서 전달받은 snapshotBitmap 사용 - stableKey=$stableKey")
            snapshotBitmap
        } else {
            // 내부에서 생성하는 경우: rememberKey로 관리
            remember(rememberKey) {
                Timber.tag(TAG_THUMBNAIL).d("remember 초기화 - internalSnapshotBitmap, key=$rememberKey")
                mutableStateOf<Bitmap?>(null)
            }
        }

    // 스냅샷 비트맵을 캐시하여 한 번 생성되면 절대 null로 변경되지 않도록 보호
    // 리컴포지션 시에도 스냅샷이 유지되도록 함
    val cachedSnapshotBitmap =
        remember(rememberKey) {
            mutableStateOf<Bitmap?>(null)
        }

    // 스냅샷 비트맵이 생성되면 캐시에 저장
    androidx.compose.runtime.LaunchedEffect(snapshotBitmapState.value) {
        snapshotBitmapState.value?.let { bitmap ->
            if (cachedSnapshotBitmap.value == null) {
                cachedSnapshotBitmap.value = bitmap
                Timber.tag(TAG_THUMBNAIL).d("스냅샷 비트맵 캐시 저장: ${bitmap.width}x${bitmap.height}")
            }
        }
    }

    // 캐시된 스냅샷이 있으면 우선 사용, 없으면 현재 상태 사용
    val displaySnapshot = cachedSnapshotBitmap.value ?: snapshotBitmapState.value
    // mapView.start() 호출 여부를 추적 (한 번만 호출되도록 보장)
    val mapStarted =
        remember(rememberKey) {
            Timber.tag(TAG_THUMBNAIL).d("remember 초기화 - mapStarted, key=$rememberKey")
            mutableStateOf(false)
        }
    // MapView가 준비되었는지 추적 (로딩 인디케이터 제어용)
    val mapReady =
        remember(rememberKey) {
            Timber.tag(TAG_THUMBNAIL).d("remember 초기화 - mapReady, key=$rememberKey")
            mutableStateOf(false)
        }
    // mapViewRef, currentLocation, locationRequested도 rememberKey를 사용하여 안정적으로 유지
    val mapViewRef =
        remember(rememberKey) {
            Timber.tag(TAG_THUMBNAIL).d("remember 초기화 - mapViewRef, key=$rememberKey")
            mutableStateOf<MapView?>(null)
        }
    val currentLocation =
        remember(rememberKey) {
            Timber.tag(TAG_THUMBNAIL).d("remember 초기화 - currentLocation, key=$rememberKey")
            mutableStateOf<LocationPoint?>(null)
        }
    val locationRequested =
        remember(rememberKey) {
            Timber.tag(TAG_THUMBNAIL).d("remember 초기화 - locationRequested, key=$rememberKey")
            mutableStateOf(false)
        }

    // 현재 위치 가져오기 (경로가 없을 때 한 번만 실행)
    LaunchedEffect(locations.isEmpty()) {
        if (locations.isEmpty() && currentLocation.value == null && !locationRequested.value) {
            locationRequested.value = true
            try {
                // LocationManager를 사용하여 현재 위치 가져오기
                val location = locationManagerInstance.getCurrentLocationOrLast()
                location?.let {
                    currentLocation.value = it
                    Timber.tag(TAG_THUMBNAIL)
                        .d("현재 위치 가져오기 성공: ${it.latitude}, ${it.longitude}")
                } ?: run {
                    Timber.tag(TAG_THUMBNAIL).w("위치를 가져올 수 없습니다")
                }
            } catch (e: Exception) {
                Timber.tag(TAG_THUMBNAIL).e(e, "현재 위치 가져오기 중 오류 발생")
            }
        }
    }

    // MapView lifecycle 관리
    // 스냅샷이 생성되면 MapView는 더 이상 필요 없으므로 정리
    androidx.compose.runtime.DisposableEffect(rememberKey) {
        onDispose {
            mapViewRef.value?.let { mapView ->
                try {
                    // 스냅샷이 생성되지 않았을 때만 정리 (스냅샷 생성 시 이미 정리됨)
                    if (snapshotBitmapState.value == null) {
                        mapView.pause()
                        Timber.tag(TAG_THUMBNAIL).d("DisposableEffect에서 MapView 정리")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG_THUMBNAIL).e(e, "MapView lifecycle 관리 실패")
                }
            }
            mapViewRef.value = null
        }
    }

    // 디버깅: 리컴포지션 및 snapshotBitmap 상태 추적
    androidx.compose.runtime.LaunchedEffect(Unit) {
        Timber.tag(TAG_THUMBNAIL).d("LaunchedEffect 실행 - 리컴포지션 발생")
    }

    androidx.compose.runtime.LaunchedEffect(snapshotBitmapState.value) {
        Timber
            .tag(
                TAG_THUMBNAIL,
            ).d(
                "snapshotBitmap 변경 감지 - ${if (snapshotBitmapState.value != null) "있음(${snapshotBitmapState.value!!.width}x${snapshotBitmapState.value!!.height})" else "null로 변경됨"}",
            )
    }

    // 리컴포지션마다 현재 상태 로그
    androidx.compose.runtime.SideEffect {
        Timber
            .tag(
                TAG_THUMBNAIL,
            ).d(
                "SideEffect 실행 (리컴포지션) - snapshotBitmap=${if (snapshotBitmapState.value != null) "있음" else "null"}, locationsKey=$locationsKey, rememberKey=$rememberKey",
            )
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 썸네일은 한 번 생성되면 변하지 않으므로, 스냅샷이 생성되었으면 그대로 유지
            // displaySnapshot을 사용하여 리컴포지션 시에도 안정적으로 유지
            val currentSnapshot = displaySnapshot
            if (currentSnapshot != null) {
                // 스냅샷 비트맵이 있으면 Image 표시 (가장 간단)
                // 스냅샷이 생성되면 MapView는 더 이상 필요 없으므로 표시하지 않음
                Timber
                    .tag(
                        TAG_THUMBNAIL,
                    ).d(
                        "Image 렌더링 - bitmap=${currentSnapshot.width}x${currentSnapshot.height}, locationsKey=$locationsKey",
                    )
                Image(
                    bitmap = currentSnapshot.asImageBitmap(),
                    contentDescription = "경로 썸네일",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // 스냅샷 생성 중: MapView와 로딩 인디케이터를 함께 표시
                Timber.tag(TAG_THUMBNAIL).d("스냅샷 생성 중 - snapshotBitmap=null, mapReady=${mapReady.value}")

                // Box를 사용하여 MapView와 로딩 인디케이터를 겹쳐서 표시
                Box(modifier = Modifier.fillMaxSize()) {
                    // 스냅샷 생성 전: MapView 렌더링 (스냅샷 생성을 위해 필요)
                    // MapView는 VISIBLE로 유지하여 초기화 보장
                    AndroidView(
                        factory = { ctx ->
                            val mapView =
                                MapView(ctx).apply {
                                    layoutParams =
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                        )
                                    // MapView는 VISIBLE로 설정 (초기화를 위해 필요)
                                    visibility = android.view.View.VISIBLE
                                }

                            // MapView 참조 저장 (LaunchedEffect에서 사용)
                            mapViewRef.value = mapView
                            Timber.tag(TAG_THUMBNAIL).d("MapView factory 실행 (VISIBLE)")

                            mapView
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { mapView ->
                            // 스냅샷이 이미 있으면 MapView를 숨기고 정리
                            if (displaySnapshot != null) {
                                Timber.tag(TAG_THUMBNAIL).d("스냅샷이 이미 있음 - MapView 정리")
                                try {
                                    mapView.pause()
                                    mapViewRef.value = null
                                } catch (e: Exception) {
                                    Timber.tag(TAG_THUMBNAIL).e(e, "MapView 정리 실패")
                                }
                                return@AndroidView
                            }

                            // MapView는 항상 VISIBLE로 유지 (초기화를 위해 필요)
                            // onMapReady에서 INVISIBLE로 변경됨
                            if (mapView.visibility != android.view.View.VISIBLE) {
                                mapView.visibility = android.view.View.VISIBLE
                                Timber.tag(TAG_THUMBNAIL).d("MapView update 호출 - VISIBLE로 강제 설정 (초기화 보장)")
                            }
                        },
                    )

                    // 로딩 인디케이터는 MapView 위에 표시 (Z-order)
                    // 배경색 없이 로딩 인디케이터만 표시하여 MapView가 초기화될 수 있도록 함
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        // 로딩 인디케이터 표시 (배경색 없음)
                        CustomProgressIndicator(
                            size = ProgressIndicatorSize.Medium,
                        )
                    }
                }

                // mapView.start()를 한 번만 호출하도록 LaunchedEffect 사용
                // update 블록에서 호출하면 리컴포지션마다 재호출될 수 있음
                // mapViewRef.value가 설정된 후에 실행되도록 key로 지정하고, mapStarted로 중복 호출 방지
                LaunchedEffect(mapViewRef.value) {
                    // mapStarted.value로 중복 호출 방지 (mapViewRef.value가 변경되어도 한 번만 실행)
                    // displaySnapshot을 확인하여 스냅샷이 이미 생성되었는지 확인
                    if (!mapStarted.value && mapViewRef.value != null && displaySnapshot == null) {
                        val mapView = mapViewRef.value!!

                        // MapView가 레이아웃될 때까지 대기 (크기가 0이면 onMapReady가 호출되지 않을 수 있음)
                        var retryCount = 0
                        while (mapView.width == 0 || mapView.height == 0) {
                            kotlinx.coroutines.delay(50)
                            retryCount++
                            if (retryCount > 40) { // 최대 2초 대기
                                Timber
                                    .tag(
                                        TAG_THUMBNAIL,
                                    ).w("MapView 레이아웃 대기 타임아웃 - width=${mapView.width}, height=${mapView.height}")
                                break
                            }
                        }

                        if (mapView.width > 0 && mapView.height > 0) {
                            Timber
                                .tag(
                                    TAG_THUMBNAIL,
                                ).d("MapView 레이아웃 완료 - width=${mapView.width}, height=${mapView.height}")
                        } else {
                            Timber
                                .tag(
                                    TAG_THUMBNAIL,
                                ).w("MapView 크기가 0입니다 - width=${mapView.width}, height=${mapView.height}, 그래도 시작 시도")
                        }

                        // MapView가 화면에 보이도록 VISIBLE로 설정 (onMapReady 호출을 위해 필요)
                        mapView.visibility = android.view.View.VISIBLE
                        // 레이아웃이 완료된 후 약간의 지연을 두어 MapView가 완전히 렌더링되도록 함
                        kotlinx.coroutines.delay(100)

                        mapStarted.value = true
                        Timber
                            .tag(
                                TAG_THUMBNAIL,
                            ).d(
                                "LaunchedEffect에서 mapView.start() 호출 - mapViewRef=${mapViewRef.value != null}, size=${mapView.width}x${mapView.height}, visibility=${mapView.visibility}",
                            )

                        // MapView 시작 타임아웃 (10초 후에도 준비되지 않으면 mapReady를 true로 설정하고 스냅샷 생성 시도)
                        launch {
                            kotlinx.coroutines.delay(10000)
                            if (!mapReady.value) {
                                Timber.tag(TAG_THUMBNAIL).w("MapView 시작 타임아웃 (10초) - mapReady를 true로 설정하고 스냅샷 생성 시도")
                                mapReady.value = true
                                // 타임아웃 시에도 스냅샷 생성 시도
                                if (mapView.width > 0 && mapView.height > 0) {
                                    try {
                                        val bitmap =
                                            android.graphics.Bitmap.createBitmap(
                                                mapView.width,
                                                mapView.height,
                                                android.graphics.Bitmap.Config.ARGB_8888,
                                            )
                                        val canvas = android.graphics.Canvas(bitmap)
                                        mapView.draw(canvas)
                                        // 부모의 snapshotBitmap을 먼저 업데이트 (리컴포지션 트리거)
                                        if (snapshotBitmap != null) {
                                            snapshotBitmap.value = bitmap
                                            Timber
                                                .tag(
                                                    TAG_THUMBNAIL,
                                                ).d("부모 snapshotBitmap 업데이트 완료: ${bitmap.width}x${bitmap.height}")
                                        }
                                        // snapshotBitmapState도 업데이트 (참조가 같은 경우에도 명시적으로)
                                        snapshotBitmapState.value = bitmap
                                        Timber
                                            .tag(
                                                TAG_THUMBNAIL,
                                            ).d(
                                                "타임아웃 후 스냅샷 생성 성공: ${bitmap.width}x${bitmap.height}, snapshotBitmapState.value=${if (snapshotBitmapState.value != null) "설정됨(${snapshotBitmapState.value!!.width}x${snapshotBitmapState.value!!.height})" else "null"}",
                                            )
                                    } catch (e: Exception) {
                                        Timber.tag(TAG_THUMBNAIL).e(e, "타임아웃 후 스냅샷 생성 실패")
                                    }
                                }
                            }
                        }

                        mapView.start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() {
                                    Timber.tag(TAG_THUMBNAIL).d("MapView destroyed")
                                }

                                override fun onMapError(error: Exception) {
                                    Timber.tag(TAG_THUMBNAIL).e(error, "MapView error")
                                    // 에러 발생 시에도 mapReady를 true로 설정하여 로딩 인디케이터 제거
                                    mapReady.value = true
                                }
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(kakaoMap: KakaoMap) {
                                    mapReady.value = true // MapView 준비 완료
                                    Timber
                                        .tag(
                                            TAG_THUMBNAIL,
                                        ).d(
                                            "MapView ready - locations.size=${locations.size}, currentLocation=${currentLocation.value != null}",
                                        )

                                    // MapView가 준비되면 INVISIBLE로 변경하여 사용자에게는 보이지 않도록 함
                                    mapView.visibility = android.view.View.INVISIBLE
                                    Timber.tag(TAG_THUMBNAIL).d("MapView를 INVISIBLE로 변경")

                                    // 경로가 있으면 그리기
                                    if (locations.isNotEmpty()) {
                                        drawPathForThumbnail(kakaoMap, locations)
                                    }

                                    // 카메라 이동: 경로가 있으면 경로 사용, 없으면 현재 위치 사용, 둘 다 없으면 emptyList 전달 (서울 기본 위치 표시)
                                    val locationsToUse =
                                        if (locations.isNotEmpty()) {
                                            locations
                                        } else if (currentLocation.value != null) {
                                            listOf(currentLocation.value!!)
                                        } else {
                                            // 위치 정보가 없으면 emptyList 전달 (moveCameraForThumbnail에서 서울 기본 위치 표시)
                                            Timber.tag(TAG_THUMBNAIL).d("위치 정보 없음 - 서울 기본 위치로 표시")
                                            emptyList()
                                        }

                                    moveCameraForThumbnail(kakaoMap, locationsToUse) {
                                        // 카메라 이동 완료 후 스냅샷 생성
                                        mapView.post {
                                            kotlinx.coroutines
                                                .CoroutineScope(
                                                    kotlinx.coroutines.Dispatchers.Main,
                                                ).launch {
                                                    // 지도 타일 로딩 대기 (서울 기본 위치도 충분히 로딩되도록)
                                                    // locationsToUse가 비어있으면 서울 기본 위치이므로 더 긴 대기 시간 필요
                                                    val delayTime =
                                                        if (locationsToUse.isEmpty()) {
                                                            2500L // 서울 기본 위치: 2.5초 대기
                                                        } else {
                                                            2000L // 경로가 있는 경우: 2초 대기
                                                        }
                                                    kotlinx.coroutines.delay(delayTime)

                                                    // MapView가 레이아웃되었는지 확인
                                                    if (mapView.width > 0 && mapView.height > 0) {
                                                        try {
                                                            // 스냅샷 생성 직전에 MapView를 잠시 VISIBLE로 설정 (타일 로딩 확인용)
                                                            // INVISIBLE 상태에서도 draw()는 작동하지만, 타일이 제대로 로드되었는지 확인하기 위해
                                                            mapView.visibility = android.view.View.VISIBLE
                                                            kotlinx.coroutines.delay(100) // 잠시 대기하여 타일 렌더링 확인

                                                            // 스냅샷 생성
                                                            val bitmap =
                                                                android.graphics.Bitmap.createBitmap(
                                                                    mapView.width,
                                                                    mapView.height,
                                                                    android.graphics.Bitmap.Config.ARGB_8888,
                                                                )
                                                            val canvas = android.graphics.Canvas(bitmap)
                                                            mapView.draw(canvas)

                                                            // 스냅샷 생성 후 다시 INVISIBLE로 설정
                                                            mapView.visibility = android.view.View.INVISIBLE

                                                            // 스냅샷이 제대로 생성되었는지 확인
                                                            val isValidSnapshot = bitmap.width > 0 && bitmap.height > 0

                                                            // 비트맵이 비어있지 않은지 확인 (모든 픽셀이 투명하거나 검은색이 아닌지)
                                                            var hasContent = false
                                                            if (isValidSnapshot) {
                                                                // 샘플링하여 비트맵에 실제 내용이 있는지 확인
                                                                val sampleSize = 10
                                                                val stepX = bitmap.width / sampleSize
                                                                val stepY = bitmap.height / sampleSize
                                                                for (x in 0 until sampleSize) {
                                                                    for (y in 0 until sampleSize) {
                                                                        val pixel =
                                                                            bitmap.getPixel(
                                                                                x * stepX,
                                                                                y * stepY,
                                                                            )
                                                                        val alpha = android.graphics.Color.alpha(pixel)
                                                                        // 알파가 0이 아니고, 완전히 검은색이 아닌 경우
                                                                        if (alpha > 0 &&
                                                                            pixel != android.graphics.Color.BLACK
                                                                        ) {
                                                                            hasContent = true
                                                                            break
                                                                        }
                                                                    }
                                                                    if (hasContent) break
                                                                }
                                                            }

                                                            if (isValidSnapshot && hasContent) {
                                                                // 부모의 snapshotBitmap을 먼저 업데이트 (리컴포지션 트리거)
                                                                if (snapshotBitmap != null) {
                                                                    snapshotBitmap.value = bitmap
                                                                    Timber
                                                                        .tag(
                                                                            TAG_THUMBNAIL,
                                                                        ).d(
                                                                            "부모 snapshotBitmap 업데이트 완료: ${bitmap.width}x${bitmap.height}",
                                                                        )
                                                                }
                                                                // 스냅샷 비트맵 설정 (부모에서 관리하는 경우에도 업데이트)
                                                                Timber
                                                                    .tag(
                                                                        TAG_THUMBNAIL,
                                                                    ).d(
                                                                        "스냅샷 생성 성공: ${bitmap.width}x${bitmap.height}, locationsKey=$locationsKey, locationsToUse.size=${locationsToUse.size}",
                                                                    )
                                                                snapshotBitmapState.value = bitmap
                                                                Timber
                                                                    .tag(
                                                                        TAG_THUMBNAIL,
                                                                    ).d(
                                                                        "스냅샷 저장 완료 - snapshotBitmapState.value=${if (snapshotBitmapState.value != null) "설정됨(${snapshotBitmapState.value!!.width}x${snapshotBitmapState.value!!.height})" else "null"}",
                                                                    )

                                                                // 스냅샷 생성 후 MapView 정리 (회색 화면 방지)
                                                                try {
                                                                    mapView.pause()
                                                                    mapViewRef.value = null // 참조 해제
                                                                    Timber
                                                                        .tag(
                                                                            TAG_THUMBNAIL,
                                                                        ).d("스냅샷 생성 후 MapView 정리 완료")
                                                                } catch (e: Exception) {
                                                                    Timber
                                                                        .tag(
                                                                            TAG_THUMBNAIL,
                                                                        ).e(e, "스냅샷 생성 후 MapView 정리 실패")
                                                                }
                                                            } else {
                                                                Timber
                                                                    .tag(
                                                                        TAG_THUMBNAIL,
                                                                    ).w(
                                                                        "스냅샷이 유효하지 않음: width=${bitmap.width}, height=${bitmap.height}, hasContent=$hasContent",
                                                                    )
                                                                // 스냅샷 생성 실패 시 재시도 또는 기본 이미지 표시
                                                                // 일단 MapView는 정리하지 않고 유지 (재시도 가능하도록)
                                                            }
                                                        } catch (e: Exception) {
                                                            Timber.tag(TAG_THUMBNAIL).e(e, "스냅샷 생성 실패: ${e.message}")
                                                        }
                                                    } else {
                                                        Timber
                                                            .tag(
                                                                TAG_THUMBNAIL,
                                                            ).w("MapView 크기가 0입니다: ${mapView.width}x${mapView.height}")
                                                    }
                                                }
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 썸네일용 경로 그리기
 */
private fun drawPathForThumbnail(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
) {
    if (locations.size < 2) return

    try {
        val shapeManager: ShapeManager? = kakaoMap.getShapeManager()
        if (shapeManager == null) {
            Timber.tag(TAG_THUMBNAIL).e("ShapeManager를 가져올 수 없습니다")
            return
        }

        val shapeLayer: ShapeLayer = shapeManager.getLayer()

        val latLngList =
            locations.map { location ->
                LatLng.from(location.latitude, location.longitude)
            }

        val mapPoints = MapPoints.fromLatLng(latLngList)

        val polylineStyle =
            PolylineStyle.from(
                8f, // 썸네일용으로 약간 얇게
                android.graphics.Color.parseColor("#4285F4"),
            )

        val polylineStyles = PolylineStyles.from(polylineStyle)
        val polylineOptions = PolylineOptions.from(mapPoints, polylineStyles)

        shapeLayer.addPolyline(polylineOptions)

        Timber.tag(TAG_THUMBNAIL).d("썸네일 경로 그리기 완료: ${locations.size}개 포인트")
    } catch (e: Exception) {
        Timber.tag(TAG_THUMBNAIL).e(e, "썸네일 경로 그리기 실패")
    }
}

/**
 * 썸네일용 카메라 이동
 * 경로가 없어도 기본 위치(서울 중심)로 지도를 표시합니다.
 */
private fun moveCameraForThumbnail(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
    onComplete: () -> Unit,
) {
    try {
        val centerLat: Double
        val centerLon: Double
        val zoomLevel: Int

        if (locations.isEmpty()) {
            // 경로가 없으면 기본 위치(서울 중심)로 표시
            centerLat = 37.5665 // 서울시청
            centerLon = 126.9780
            zoomLevel = 15
            Timber.tag(TAG_THUMBNAIL).d("경로가 없어서 기본 위치(서울 중심)로 지도 표시")
        } else {
            // 경로가 있으면 경로 중심으로 표시
            var minLat = locations[0].latitude
            var maxLat = locations[0].latitude
            var minLon = locations[0].longitude
            var maxLon = locations[0].longitude

            locations.forEach { location ->
                minLat = minOf(minLat, location.latitude)
                maxLat = maxOf(maxLat, location.latitude)
                minLon = minOf(minLon, location.longitude)
                maxLon = maxOf(maxLon, location.longitude)
            }

            val latRange = maxLat - minLat
            val lonRange = maxLon - minLon

            // 중앙 좌표 계산
            centerLat = (minLat + maxLat) / 2
            centerLon = (minLon + maxLon) / 2

            // 줌 레벨 계산
            zoomLevel =
                when {
                    latRange < 0.001 || lonRange < 0.001 -> 18
                    latRange < 0.01 || lonRange < 0.01 -> 16
                    latRange < 0.1 || lonRange < 0.1 -> 14
                    else -> 12
                }
        }

        val centerPosition = LatLng.from(centerLat, centerLon)

        // 카메라 업데이트 생성 (중앙 위치 + 줌 레벨)
        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newCenterPosition(centerPosition, zoomLevel)

        // 카메라 이동
        try {
            kakaoMap.moveCamera(cameraUpdate)
            Timber
                .tag(
                    TAG_THUMBNAIL,
                ).d("썸네일 카메라 이동 요청: 중심 ($centerLat, $centerLon), 줌 레벨: $zoomLevel, locations.size=${locations.size}")
        } catch (e: Exception) {
            Timber.tag(TAG_THUMBNAIL).e(e, "썸네일 카메라 이동 실패: ${e.message}")
            // 실패해도 onComplete 호출하여 스냅샷 생성 시도
            onComplete()
            return
        }

        // 카메라 이동 완료 대기 (서울 기본 위치의 경우 타일 로딩을 위해 더 긴 대기 시간 필요)
        val delayTime =
            if (locations.isEmpty()) {
                1000L // 서울 기본 위치: 1초 대기
            } else {
                500L // 경로가 있는 경우: 0.5초 대기
            }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Timber.tag(TAG_THUMBNAIL).d("카메라 이동 완료 콜백 실행 - locations.size=${locations.size}")
            onComplete()
        }, delayTime)
    } catch (e: Exception) {
        Timber.tag(TAG_THUMBNAIL).e(e, "썸네일 카메라 이동 실패")
        onComplete()
    }
}
