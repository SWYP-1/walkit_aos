package team.swyp.sdu.ui.customtest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.KakaoMapView
import team.swyp.sdu.domain.service.filter.PathSmoother
import team.swyp.sdu.utils.WalkingTestData
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.ui.theme.WalkItTheme
import timber.log.Timber

/**
 * 경로 스무딩 테스트 화면
 *
 * 카카오 맵을 사용하여 4가지 다른 경로 필터링 옵션을 비교합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTestScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 테스트용 더미 데이터 생성
    val testData by remember {
        mutableStateOf(WalkingTestData.generateTestSessions().random())
    }

    // 경로 데이터 준비
    val pathData by remember(testData) {
        mutableStateOf(
            testData?.locations ?: emptyList()
        )
    }

    // PathSmoother 인스턴스
    val pathSmoother = remember { PathSmoother() }

    // 탭 상태
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 각 탭별 경로 데이터
    val tabPaths by remember(pathData, pathSmoother) {
        mutableStateOf(
            if (pathData.isNotEmpty()) {
                createTabPaths(pathData, pathSmoother)
            } else {
                List(4) { emptyList<LocationPoint>() }
            }
        )
    }

    val tabTitles = listOf(
        "원본 경로",
        "PolyUtil만",
        "SphericalUtil만",
        "PolyUtil + SphericalUtil"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // 헤더
        AppHeader(
            title = "경로 스무딩 테스트",
            onNavigateBack = onNavigateBack
        )

        // 탭 레이아웃
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // 지도 영역
        Box(modifier = Modifier.weight(1f)) {
            if (pathData.isEmpty()) {
                // 데이터가 없는 경우
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("테스트용 경로 데이터가 없습니다")
                    Text("더미 세션 데이터를 먼저 추가해주세요")
                }
            } else {
                // 카카오 맵뷰로 선택된 탭의 경로 표시
                KakaoMapView(
                    locations = tabPaths[selectedTabIndex],
                    modifier = Modifier.fillMaxSize()
                )

                // 디버그 정보 오버레이
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = tabTitles[selectedTabIndex],
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "포인트: ${tabPaths[selectedTabIndex].size}개",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * 각 탭별 경로 데이터를 생성합니다.
 */
private fun createTabPaths(
    originalPath: List<LocationPoint>,
    pathSmoother: PathSmoother
): List<List<LocationPoint>> {
    val results = mutableListOf<List<LocationPoint>>()

    // 위도/경도 리스트로 변환
    val latitudes = originalPath.map { it.latitude }
    val longitudes = originalPath.map { it.longitude }

    // 1. 원본 경로
    results.add(originalPath)

    // 2. PolyUtil만 적용 (단순화)
    val polyUtilResult = pathSmoother.smoothPath(
        latitudes = latitudes,
        longitudes = longitudes,
        simplifyTolerance = 5.0,
        smoothSegments = 1  // 보간 없음
    )
    results.add(createLocationPointsFromCoords(polyUtilResult.first, polyUtilResult.second, originalPath))

    // 3. SphericalUtil만 적용 (보간만)
    val sphericalResult = pathSmoother.smoothPath(
        latitudes = latitudes,
        longitudes = longitudes,
        simplifyTolerance = Double.MAX_VALUE,  // 단순화 없음
        smoothSegments = 5  // 보간만 적용
    )
    results.add(createLocationPointsFromCoords(sphericalResult.first, sphericalResult.second, originalPath))

    // 4. PolyUtil + SphericalUtil 둘다 적용 (완전 스무딩)
    val fullSmoothResult = pathSmoother.smoothPath(
        latitudes = latitudes,
        longitudes = longitudes,
        simplifyTolerance = 5.0,  // 단순화 적용
        smoothSegments = 5  // 보간 적용
    )
    results.add(createLocationPointsFromCoords(fullSmoothResult.first, fullSmoothResult.second, originalPath))

    Timber.d("경로 스무딩 테스트 데이터 생성 완료:")
    results.forEachIndexed { index, path ->
        Timber.d("  탭 ${index + 1}: ${path.size}개 포인트")
    }

    return results
}

/**
 * 위도/경도 좌표 리스트로부터 LocationPoint 리스트를 생성합니다.
 */
private fun createLocationPointsFromCoords(
    latitudes: List<Double>,
    longitudes: List<Double>,
    originalPoints: List<LocationPoint>
): List<LocationPoint> {
    return latitudes.zip(longitudes).mapIndexed { index, (lat, lng) ->
        // 원본 포인트의 타임스탬프와 정확도를 재사용 (가능한 경우)
        val originalPoint = originalPoints.getOrNull(index)
        LocationPoint(
            latitude = lat,
            longitude = lng,
            timestamp = originalPoint?.timestamp ?: System.currentTimeMillis(),
            accuracy = originalPoint?.accuracy
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun MapTestScreenPreview() {
    WalkItTheme {
        MapTestScreen(
            onNavigateBack = {}
        )
    }
}
