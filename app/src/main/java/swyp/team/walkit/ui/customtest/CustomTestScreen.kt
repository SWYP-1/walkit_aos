package swyp.team.walkit.ui.customtest

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.presentation.viewmodel.CameraSettings
import swyp.team.walkit.presentation.viewmodel.KakaoMapViewModel
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.KakaoMapView
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.utils.loadLocationsFromJson
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 랜덤한 산책 경로를 생성합니다.
 * 시작점을 기준으로 자연스러운 산책 경로를 생성합니다.
 *
 * @param startLat 시작 위도 (서울 중심부: 37.5665)
 * @param startLon 시작 경도 (서울 중심부: 126.9780)
 * @param pointCount 생성할 포인트 개수 (1000개 이상 권장)
 * @param maxDistanceKm 최대 이동 거리 (km)
 * @return 생성된 LocationPoint 리스트
 */

fun generateRandomWalkingPath(
    startLat: Double = 37.5665,
    startLon: Double = 126.9780,
    pointCount: Int = 1200,
    maxDistanceKm: Double = 5.0
): List<LocationPoint> {
    val points = mutableListOf<LocationPoint>()
    var currentLat = startLat
    var currentLon = startLon
    var currentTime = System.currentTimeMillis()

    // 시작점 추가
    points.add(LocationPoint(
        latitude = currentLat,
        longitude = currentLon,
        timestamp = currentTime
    ))

    // 랜덤 방향으로 이동하며 포인트 생성
    for (i in 1 until pointCount) {
        // 랜덤한 방향과 거리 계산
        val angle = Random.nextDouble() * 2 * PI // 0-360도 랜덤
        val distance = Random.nextDouble() * 0.01 // 최대 10m 이동 (너무 빠른 이동 방지)

        // 위도/경도 변환 (단순 근사)
        val deltaLat = distance * cos(angle) / 111.0 // 위도 1도 ≈ 111km
        val deltaLon = distance * sin(angle) / (111.0 * cos(currentLat * PI / 180.0)) // 경도 변환

        currentLat += deltaLat
        currentLon += deltaLon
        currentTime += 1000 // 1초 간격

        // 서울 중앙 지역 내로 제한 (경로가 테두리에 걸리지 않도록 더 중앙 집중)
        currentLat = currentLat.coerceIn(37.5, 37.65)
        currentLon = currentLon.coerceIn(126.9, 127.1)

        points.add(LocationPoint(
            latitude = currentLat,
            longitude = currentLon,
            timestamp = currentTime
        ))
    }

    return points
}

/**
 * 커스텀 테스트 화면
 *
 * 온보딩 실행을 위한 테스트 화면입니다.
 */
@Composable
fun CustomTestScreen(
    onNavigateBack: () -> Unit = {},
    onStartOnboarding: () -> Unit = {},
    onAddDummySessions: () -> Unit = {},
    onNavigateToZoomTest: () -> Unit = {},
    onNavigateToRandomPath: () -> Unit = {},
    onNavigateToLatLngBoundsTest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showSuccessMessage by remember { mutableStateOf(false) }

    // 데이터 추가 성공 시 스낵바 표시
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "더미 데이터 40개가 성공적으로 추가되었습니다!\n홈 화면이나 산책 기록 화면에서 확인해주세요.\n\n💡 표시되지 않으면 화면을 아래로 당겨 새로고침해보세요.",
                    actionLabel = "확인"
                )
            }
            showSuccessMessage = false
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 헤더
            AppHeader(
                title = "커스텀 테스트",
                onNavigateBack = onNavigateBack,
            )

            // 콘텐츠
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "개발자용 테스트 메뉴",
                    style = MaterialTheme.walkItTypography.headingM,
                )

                Text(
                    text = "온보딩 플로우와 더미 데이터를 테스트할 수 있습니다.",
                    style = MaterialTheme.walkItTypography.bodyM,
                )
            }

            // 더미 세션 추가 버튼
            CtaButton(
                text = "더미 세션 데이터 추가 (40개)",
                onClick = {
                    onAddDummySessions()
                    showSuccessMessage = true
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 줌 레벨 계산 테스트 버튼
            CtaButton(
                text = "줌 레벨 계산 테스트",
                onClick = onNavigateToZoomTest,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 랜덤 경로 생성 테스트 버튼
            CtaButton(
                text = "랜덤 경로 생성 테스트",
                onClick = onNavigateToRandomPath,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // LatLngBounds 테스트 버튼
            CtaButton(
                text = "LatLngBounds 카메라 테스트",
                onClick = onNavigateToLatLngBoundsTest,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 온보딩 시작 버튼
            CtaButton(
                text = "온보딩으로 가기",
                onClick = onStartOnboarding,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // 스낵바 호스트
        SnackbarHost(hostState = snackbarHostState)
    }
}




/**
 * LatLngBounds 카메라 테스트 화면
 *
 * LatLngBounds를 사용해서 경로가 테두리에 걸리지 않도록 자동으로 카메라를 조정하는 테스트
 */

fun calculateTotalDistance(locations: List<LocationPoint>): Float {
    if (locations.size < 2) {
        return 0f
    }

    var totalDistance = 0f
    val results = FloatArray(1)

    for (i in 0 until locations.size - 1) {
        val start = locations[i]
        val end = locations[i + 1]

        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            results
        )

        totalDistance += results[0]
    }

    return totalDistance
}

@Composable
fun LatLngBoundsTestScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapViewModel = remember { KakaoMapViewModel() }
    var randomLocations by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var pathStats by remember { mutableStateOf<PathStats?>(null) }
    var currentPaddingPx by remember { mutableStateOf(64) } // LatLngBounds 패딩 (픽셀)
    var updateTrigger by remember { mutableStateOf(0) } // 강제 업데이트 트리거

    val density = LocalDensity.current

    fun generateRandomPath(density: Density) {
        isGenerating = true

        // 기존 경로 초기화
        mapViewModel.clearPaths()
        pathStats = null

        // 랜덤 경로 생성 (LatLngBounds 테스트용)
        val locations = generateRandomWalkingPath(
            pointCount = 800, // 테스트용 적당한 크기
            maxDistanceKm = 2.0
        )

        randomLocations = locations

        // 통계 계산
        val totalDistance = calculateTotalDistance(locations)
        val duration = locations.last().timestamp - locations.first().timestamp
        val avgSpeed = if (duration > 0) (totalDistance / 1000) / (duration / 3600000.0) else 0.0 // km/h

        val latitudes = locations.map { it.latitude }
        val longitudes = locations.map { it.longitude }
        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 0.0
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        pathStats = PathStats(
            pointCount = locations.size,
            totalDistance = totalDistance.toDouble(),
            duration = duration,
            avgSpeed = avgSpeed,
            latRange = latRange,
            lonRange = lonRange,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            centerLat = centerLat,
            centerLon = centerLon
        )

        // ViewModel에 경로 설정 (LatLngBounds가 자동으로 카메라를 조정함)
        mapViewModel.setLocations(locations, density)
        updateTrigger++ // 강제 업데이트 트리거
        isGenerating = false
    }

    LaunchedEffect(Unit) {
        generateRandomPath(density)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 헤더
        AppHeader(
            title = "LatLngBounds 카메라 테스트",
            onNavigateBack = onNavigateBack,
        )

        if (isGenerating) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "랜덤 경로 생성 중...",
                    style = MaterialTheme.walkItTypography.bodyM,
                )
            }
        } else {
            // 컨트롤 패널
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LatLngBounds 자동 카메라 테스트",
                    style = MaterialTheme.walkItTypography.headingM,
                )

                Text(
                    text = "패딩 설정: ${currentPaddingPx}px",
                    style = MaterialTheme.walkItTypography.bodyM,
                    color = MaterialTheme.colorScheme.secondary
                )

                // 패딩 조절 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CtaButton(
                        text = "패딩 -",
                        onClick = {
                            currentPaddingPx = maxOf(0, currentPaddingPx - 16)
                            updateTrigger++ // 카메라 재조정 트리거
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CtaButton(
                        text = "패딩 +",
                        onClick = {
                            currentPaddingPx = minOf(200, currentPaddingPx + 16)
                            updateTrigger++ // 카메라 재조정 트리거
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                pathStats?.let { stats ->
                    Text(
                        text = "경로 통계",
                        style = MaterialTheme.walkItTypography.headingS,
                    )
                    Text(
                        text = "포인트 개수: ${stats.pointCount}개",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                    Text(
                        text = "총 거리: ${String.format("%.2f", stats.totalDistance / 1000)}km",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                    Text(
                        text = "위도 범위: ${String.format("%.6f", stats.minLat)} ~ ${String.format("%.6f", stats.maxLat)}",
                        style = MaterialTheme.walkItTypography.bodyS,
                    )
                    Text(
                        text = "경도 범위: ${String.format("%.6f", stats.minLon)} ~ ${String.format("%.6f", stats.maxLon)}",
                        style = MaterialTheme.walkItTypography.bodyS,
                    )

                    // LatLngBounds 보장 문구
                    Text(
                        text = "✅ LatLngBounds 보장: 모든 경로 포인트가 화면 안에 표시됩니다!",
                        style = MaterialTheme.walkItTypography.bodyM,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // 새 경로 생성 버튼
                CtaButton(
                    text = "새 랜덤 경로 생성",
                    onClick = { generateRandomPath(density) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 지도 표시 (LatLngBounds 자동 적용)
            KakaoMapView(
                locations = randomLocations,
                viewModel = mapViewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}

/**
 * 랜덤 경로 생성 테스트 화면
 *
 * 랜덤한 산책 경로를 생성해서 지도에 표시하고,
 * 경로 정보를 보여줍니다.
 */

data class PathStats(
    val pointCount: Int,
    val totalDistance: Double,
    val duration: Long,
    val avgSpeed: Double,
    val latRange: Double,
    val lonRange: Double,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double
)

@Composable
fun RandomPathTestScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mapViewModel = remember { KakaoMapViewModel() }
    var locations by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var pathStats by remember { mutableStateOf<PathStats?>(null) }
    var updateTrigger by remember { mutableStateOf(0) }

    val density = LocalDensity.current

    fun generateRandomPath() {
        isGenerating = true
        mapViewModel.clearPaths()
        pathStats = null

        // 랜덤 경로 생성 (LatLngBounds가 자동으로 화면에 맞춰줌)
        val path = generateRandomWalkingPath(
            pointCount = 1200,
            maxDistanceKm = 3.0
        )

        locations = path

        // 간단한 통계 계산
        val latitudes = path.map { it.latitude }
        val longitudes = path.map { it.longitude }
        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 0.0

        var totalDistance = 0.0
        if (path.size >= 2) {
            for (i in 1 until path.size) {
                val prev = path[i-1]
                val curr = path[i]
                val latDiff = curr.latitude - prev.latitude
                val lonDiff = curr.longitude - prev.longitude
                totalDistance += kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111000
            }
        }

        val duration = if (path.size >= 2) path.last().timestamp - path.first().timestamp else 0L
        val avgSpeed = if (duration > 0) (totalDistance / 1000) / (duration / 3600000.0) else 0.0

        pathStats = PathStats(
            pointCount = path.size,
            totalDistance = totalDistance,
            duration = duration,
            avgSpeed = avgSpeed,
            latRange = maxLat - minLat,
            lonRange = maxLon - minLon,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            centerLat = (minLat + maxLat) / 2,
            centerLon = (minLon + maxLon) / 2
        )

        // ViewModel에 설정 (LatLngBounds 자동 적용!)
        mapViewModel.setLocations(path, density)
        updateTrigger++
        isGenerating = false
    }

    // 화면 진입 시 자동 경로 생성
    LaunchedEffect(Unit) {
        generateRandomPath()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 헤더
        AppHeader(
            title = "LatLngBounds 랜덤 경로 테스트",
            onNavigateBack = onNavigateBack,
        )

        if (isGenerating) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("경로 생성 중...", style = MaterialTheme.walkItTypography.bodyM)
            }
        } else {
            // 컨트롤 패널
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "LatLngBounds 자동 카메라 테스트",
                    style = MaterialTheme.walkItTypography.headingM
                )
                Text(
                    "모든 경로 포인트가 자동으로 화면 안에 맞춰집니다!",
                    style = MaterialTheme.walkItTypography.bodyM,
                    color = MaterialTheme.colorScheme.primary
                )

                pathStats?.let { stats ->
                    Text("포인트: ${stats.pointCount}개", style = MaterialTheme.walkItTypography.bodyM)
                    Text("거리: ${String.format("%.2f", stats.totalDistance / 1000)}km", style = MaterialTheme.walkItTypography.bodyM)
                    Text("시간: ${stats.duration / 1000 / 60}분", style = MaterialTheme.walkItTypography.bodyM)
                    Text("속도: ${String.format("%.1f", stats.avgSpeed)}km/h", style = MaterialTheme.walkItTypography.bodyM)
                }

                // 새 경로 생성 버튼
                CtaButton(
                    text = "새 랜덤 경로 생성",
                    onClick = { generateRandomPath() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 지도 표시 (LatLngBounds 자동 적용)
            KakaoMapView(
                locations = locations,
                viewModel = mapViewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomTestScreenPreview() {
    WalkItTheme {
        CustomTestScreen()
    }
}
