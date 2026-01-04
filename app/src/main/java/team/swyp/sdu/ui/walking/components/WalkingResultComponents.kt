package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.route.RouteLineManager
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.utils.WalkingTestData.generateRandomCityWalkPoints
import timber.log.Timber
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 통계 아이템 컴포넌트
 */
@Composable
fun StatItem(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 주간 완료 현황 행
 */
@Composable
fun WeekCompletionRow(
    sessionsThisWeek: List<WalkingSession>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val completionMap =
        sessionsThisWeek.groupBy { session ->
            Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { true }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val labels = listOf("월", "화", "수", "목", "금", "토", "일")
        labels.forEachIndexed { index, label ->
            val date = startOfWeek.plusDays(index.toLong())
            val isDone = completionMap[date] == true
            WeekCircle(label = label, isDone = isDone)
        }
    }
}

/**
 * 주간 완료 원형 표시
 */
@Composable
fun WeekCircle(label: String, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        color = if (isDone) Color(0xFF2E2E2E) else Color(0xFFEAEAEA),
                        shape = RoundedCornerShape(50),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Text(
                    text = "✔",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * 경로 썸네일 컴포넌트
 */

@Composable
fun PathThumbnail(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    pathColor: Color = Color.White,
    startColor: Color = Color(0xFF4CAF50),
    endColor: Color = Color(0xFFF44336)
) {
    if (locations.isEmpty()) return

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val paddingPx = 16f

        // 경로 범위
        val minLat = locations.minOf { it.latitude }.toFloat()
        val maxLat = locations.maxOf { it.latitude }.toFloat()
        val minLon = locations.minOf { it.longitude }.toFloat()
        val maxLon = locations.maxOf { it.longitude }.toFloat()

        // 경로의 중심점 계산
        val centerLat = (minLat + maxLat) / 2f
        val centerLon = (minLon + maxLon) / 2f

        // 한 점/작은 범위 대비 최소값 적용
        val minRange = 0.001f
        val latRange = maxOf(maxLat - minLat, minRange)
        val lonRange = maxOf(maxLon - minLon, minRange)

        // Canvas 내 사용 가능한 영역
        val availableWidth = canvasWidth - paddingPx * 2
        val availableHeight = canvasHeight - paddingPx * 2

        // 스케일 (비율 유지)
        val scale = minOf(availableWidth / lonRange, availableHeight / latRange)

        // 캔버스의 중심점
        val canvasCenterX = canvasWidth / 2f
        val canvasCenterY = canvasHeight / 2f

        // 좌표 변환 (경로 중심을 캔버스 중심에 맞춤)
        val points = locations.map { loc ->
            val x = (canvasCenterX + (loc.longitude - centerLon) * scale).toFloat()
            val y = (canvasCenterY - (loc.latitude - centerLat) * scale).toFloat()
            Offset(x, y)
        }

        // 경로 그리기 (2점 이상)
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        // 시작점
        val startRadius = if (locations.size == 1) 12f else 10f
        drawCircle(
            color = startColor,
            radius = startRadius,
            center = points.first(),
        )

        // 끝점 (시작점과 다른 경우)
        if (points.size > 1 &&
            (points.last().x != points.first().x || points.last().y != points.first().y)
        ) {
            drawCircle(
                color = endColor,
                radius = 10f,
                center = points.last(),
            )
        }
    }
}




/**
 * 테스트용 간단한 카카오 지도 뷰
 * 위치 리스트를 받아 카카오 지도에 경로를 표시합니다.
 */
@Composable
fun MapTestView(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapStarted by remember { mutableStateOf(false) }

    // locations 변경 시 경로 업데이트
    LaunchedEffect(locations) {
        Timber.tag("MapTestView").d("지도준비: locations 변경 감지, 경로 업데이트 시작")
        kakaoMap?.let { map ->
            if (locations.size >= 2) {
                Timber.tag("MapTestView").d("경로 그리기 진입: ${locations.size}개의 위치 데이터로 경로 그리기 시작")
                drawPathOnMap(map, locations)
                Timber.tag("MapTestView").d("카메라 이동 시작: 경로에 맞게 카메라 위치 조정")
                moveCameraToPath(map, locations)
            } else {
                Timber.tag("MapTestView").d("경로 업데이트 스킵: 위치 데이터 부족 (${locations.size}개)")
            }
        } ?: Timber.tag("MapTestView").d("지도준비: KakaoMap이 아직 준비되지 않음")
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    mapView = this
                }
            },
            update = { mv ->
                if (!mapStarted) {
                    Timber.tag("MapTestView").d("지도준비: MapView 시작")
                    mapStarted = true
                    mv.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {
                                Timber.tag("MapTestView").d("지도준비: MapView 파괴")
                                // 정리 작업
                            }

                            override fun onMapError(error: Exception?) {
                                Timber.tag("MapTestView").e(error, "지도준비: MapView 에러 발생")
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(map: KakaoMap) {
                                Timber.tag("MapTestView").d("지도준비 완료: KakaoMap 준비됨")
                                kakaoMap = map

                                // 카카오맵 제스처 활성화 확인 (기본적으로 활성화되어 있음)
                                // 필요시 명시적으로 설정할 수 있지만, 기본값이 활성화 상태

                                // 경로 그리기
                                if (locations.size >= 2) {
                                    Timber.tag("MapTestView").d("경로 그리기 진입: onMapReady에서 경로 그리기 호출")
                                    drawPathOnMap(map, locations)
                                } else {
                                    Timber.tag("MapTestView").d("경로 그리기 스킵: 위치 데이터 부족 (${locations.size}개)")
                                }
                                // 카메라 이동 (경로가 잘 보이도록 패딩 포함)
                                Timber.tag("MapTestView").d("카메라 이동 진입: onMapReady에서 카메라 이동 호출")
                                moveCameraToPath(map, locations)
                            }
                        },
                    )
                }
            },
        )
    }
}

/**
 * 카카오 지도에 경로 그리기
 */
private fun drawPathOnMap(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
) {
    Timber.tag("MapTestView").d("경로 그리기 시작: ${locations.size}개의 위치 데이터를 지도에 표시")
    val routeLineManager = kakaoMap.routeLineManager ?: run {
        Timber.tag("MapTestView").e("경로 그리기 실패: RouteLineManager가 null입니다")
        return
    }

    val latLngs = locations.map {
        LatLng.from(it.latitude, it.longitude)
    }

    val style = RouteLineStyle.from(16f, android.graphics.Color.BLUE)
    val styles = RouteLineStyles.from(style)
    val stylesSet = RouteLineStylesSet.from(styles)
    val segment = RouteLineSegment.from(latLngs)
        .setStyles(stylesSet.getStyles(0))

    val routeLineOptions = RouteLineOptions.from(segment)
        .setStylesSet(stylesSet)

    routeLineManager.layer.addRouteLine(routeLineOptions) { _, _ ->
        Timber.tag("MapTestView").d("경로 그리기 완료: 지도에 경로 라인 표시 성공")
    }
}

/**
 * 카메라를 경로에 맞게 이동
 * 경로가 화면에 잘 보이도록 패딩을 추가하고 정교한 줌 레벨을 계산합니다.
 */
private fun moveCameraToPath(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
) {
    Timber.tag("MapTestView").d("카메라 이동 계산 시작: ${locations.size}개의 위치 데이터 기반으로 카메라 위치 계산")

    if (locations.isEmpty()) {
        Timber.tag("MapTestView").d("카메라 이동: 위치 데이터 없음, 기본 위치(서울)로 이동")
        // 기본 위치 (서울 중심)
        val center = LatLng.from(37.5665, 126.9780)
        kakaoMap.moveCamera(
            CameraUpdateFactory.newCenterPosition(center, 15),
        )
        Timber.tag("MapTestView").d("카메라 이동 완료: 기본 위치(서울)로 이동 완료")
        return
    }

    // 경로 범위 계산
    val minLat = locations.minOf { it.latitude }
    val maxLat = locations.maxOf { it.latitude }
    val minLon = locations.minOf { it.longitude }
    val maxLon = locations.maxOf { it.longitude }

    // 중앙 좌표 계산
    val centerLat = (minLat + maxLat) / 2
    val centerLon = (minLon + maxLon) / 2

    // 경계 범위 계산
    val latRange = maxLat - minLat
    val lonRange = maxLon - minLon

    // 패딩 추가 (경로가 화면 가장자리에 붙지 않도록 20% 여유 공간 추가)
    val paddingFactor = 1.2
    val paddedLatRange = latRange * paddingFactor
    val paddedLonRange = lonRange * paddingFactor

    // 정교한 줌 레벨 계산
    val zoomLevel = calculateZoomLevel(
        latRange = paddedLatRange,
        lonRange = paddedLonRange,
        centerLat = centerLat,
    )

    Timber.d("카메라 이동: 중심 ($centerLat, $centerLon), 줌 레벨: $zoomLevel, 경로 범위 (lat: $latRange, lon: $lonRange)")

    val center = LatLng.from(centerLat, centerLon)
    kakaoMap.moveCamera(
        CameraUpdateFactory.newCenterPosition(center, zoomLevel),
    )
    Timber.tag("MapTestView").d("카메라 이동 완료: 중심 ($centerLat, $centerLon), 줌 레벨 $zoomLevel 로 이동 완료")
}

/**
 * 경계 범위를 기반으로 적절한 줌 레벨 계산
 * 위도와 경도 범위를 고려하여 경로가 화면에 잘 보이도록 줌 레벨을 결정합니다.
 */
private fun calculateZoomLevel(
    latRange: Double,
    lonRange: Double,
    centerLat: Double,
): Int {
    // 위도 1도 ≈ 111km
    // 경도 1도 ≈ 111km * cos(위도)
    val latDegreeToKm = 111.0
    val lonDegreeToKm = 111.0 * kotlin.math.cos(kotlin.math.PI * centerLat / 180.0)

    // 경계 범위를 km로 변환
    val latRangeKm = latRange * latDegreeToKm
    val lonRangeKm = lonRange * lonDegreeToKm

    // 더 큰 범위를 기준으로 줌 레벨 계산
    val maxRangeKm = maxOf(latRangeKm, lonRangeKm)

    // 줌 레벨 계산 공식 (카카오맵 기준)
    val zoomLevel = when {
        maxRangeKm <= 0.0 -> 18 // 단일 포인트
        maxRangeKm < 0.1 -> 18 // 약 100m
        maxRangeKm < 0.5 -> 17 // 약 500m
        maxRangeKm < 1.0 -> 16 // 약 1km
        maxRangeKm < 2.0 -> 15 // 약 2km
        maxRangeKm < 5.0 -> 14 // 약 5km
        maxRangeKm < 10.0 -> 13 // 약 10km
        maxRangeKm < 20.0 -> 12 // 약 20km
        maxRangeKm < 50.0 -> 11 // 약 50km
        maxRangeKm < 100.0 -> 10 // 약 100km
        maxRangeKm < 200.0 -> 9 // 약 200km
        else -> 8 // 그 이상
    }

    // 최소/최대 줌 레벨 제한
    return zoomLevel.coerceIn(8, 18)
}

