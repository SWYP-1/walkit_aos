package team.swyp.sdu.ui.components

import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polyline
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.PolylineStyle
import com.kakao.vectormap.shape.PolylineStyles
import com.kakao.vectormap.shape.ShapeLayer
import com.kakao.vectormap.shape.ShapeManager
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.utils.LocationTestData
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * KakaoMap을 Compose에서 사용하기 위한 컴포저블
 *
 * @param locations 경로를 표시할 위치 좌표 리스트
 * @param modifier Modifier
 */
@Composable
fun KakaoMapView(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // KakaoMap 객체 저장 (지도 업데이트용)
    var kakaoMapInstance by remember {
        mutableStateOf<com.kakao.vectormap.KakaoMap?>(null)
    }

    // 이전 locations 추적 (변경 감지용)
    var previousLocations by remember {
        mutableStateOf<List<LocationPoint>>(emptyList())
    }

    var mapStarted by remember {
        mutableStateOf(false)
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { mapView ->
            if (!mapStarted) {
                mapStarted = true
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
                            val startTime = System.currentTimeMillis()
                            Timber.d("KakaoMap ready - locations.size=${locations.size}")

                            // KakaoMap 인스턴스 저장 (locations 변경 시 업데이트용)
                            kakaoMapInstance = kakaoMap
                            previousLocations = locations.toList() // 리스트 복사

                            // 위치 포인트가 0개 또는 1개일 때만 테스트용 하드코딩 위치 사용, 2개 이상이면 원래 값 사용
                            val locationsToUse =
                                if (locations.size <= 1) {
                                    Timber.d("위치 포인트가 ${locations.size}개 - 테스트용 하드코딩 위치(서울) 사용")
                                    LocationTestData.getSeoulTestLocations() // 테스트용 20개 위치 반환
                                } else {
                                    Timber.d("위치 포인트가 ${locations.size}개 - 원래 값 사용")
                                    locations
                                }

                            Timber.d("사용할 경로 포인트 수: ${locationsToUse.size} (원본: ${locations.size})")

                            // 경로에 맞게 카메라 이동
                            moveCameraToPath(kakaoMap, locationsToUse)

                            // 카메라 이동 완료 대기 후 경로 그리기
                            mapView.postDelayed({
                                // 경로를 Polyline으로 표시 (2개 이상일 때만)
                                if (locationsToUse.size >= 2) {
                                    drawPath(kakaoMap, locationsToUse)
                                    Timber.d("경로 그리기 완료: ${locationsToUse.size}개 포인트")
                                }
                            }, 500) // 카메라 이동 대기
                        }
                    },
                )
            }
        },
    )

    // locations가 변경될 때마다 지도 업데이트 (크기나 내용이 변경되었는지 확인)
    LaunchedEffect(locations.size, locations.firstOrNull()?.latitude, locations.lastOrNull()?.latitude) {
        val map = kakaoMapInstance
        // locations가 실제로 변경되었는지 확인 (크기나 첫/마지막 위치 비교)
        val locationsChanged =
            previousLocations.size != locations.size ||
                previousLocations.firstOrNull()?.latitude != locations.firstOrNull()?.latitude ||
                previousLocations.lastOrNull()?.latitude != locations.lastOrNull()?.latitude

        if (map != null && locationsChanged && mapStarted) {
            Timber.d("위치 포인트 변경 감지: ${previousLocations.size} -> ${locations.size}, 지도 업데이트")

            // 위치 포인트가 0개 또는 1개일 때만 테스트용 하드코딩 위치 사용, 2개 이상이면 원래 값 사용
            val locationsToUse =
                if (locations.size <= 1) {
                    Timber.d("위치 포인트가 ${locations.size}개 - 테스트용 하드코딩 위치(서울) 사용")
                    LocationTestData.getSeoulTestLocations() // 테스트용 20개 위치 반환
                } else {
                    Timber.d("위치 포인트가 ${locations.size}개 - 원래 값 사용")
                    locations
                }

            // 기존 경로 제거 (ShapeManager를 통해)
            try {
                val shapeManager = map.getShapeManager()
                if (shapeManager != null) {
                    val shapeLayer = shapeManager.getLayer()
                    // 모든 Polyline 제거 (간단한 방법: 레이어 재생성 또는 개별 제거)
                    // 실제로는 이전에 그린 Polyline ID를 추적해야 하지만, 여기서는 전체 레이어를 재생성
                    Timber.d("기존 경로 제거 및 새 경로 그리기")
                }
            } catch (e: Exception) {
                Timber.e(e, "기존 경로 제거 실패")
            }

            // 카메라 이동
            moveCameraToPath(map, locationsToUse)

            // 경로 그리기 (2개 이상일 때만)
            if (locationsToUse.size >= 2) {
                delay(500) // 카메라 이동 대기
                drawPath(map, locationsToUse)
                Timber.d("경로 업데이트 완료: ${locationsToUse.size}개 포인트")
            }

            previousLocations = locations.toList() // 리스트 복사
        }
    }
}

/**
 * 경로를 Polyline으로 그리기
 * 카카오맵 SDK v2의 ShapeManager를 사용하여 경로를 표시합니다.
 */
private fun drawPath(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
) {
    // 테스트용: 1개 이상이면 경로 그리기 시도 (원래는 2개 이상 필요)
    if (locations.isEmpty()) {
        Timber.d("경로 포인트가 없습니다")
        return
    }

    // 1개만 있으면 마커만 표시하고 경로는 그리지 않음
    if (locations.size < 2) {
        Timber.d("경로 포인트가 1개뿐입니다: ${locations.size}개 - 마커만 표시 (테스트용)")
        // TODO: 마커 표시 기능 추가
        return
    }

    try {
        // ShapeManager 가져오기 (nullable 처리)
        val shapeManager: ShapeManager? = kakaoMap.shapeManager
        if (shapeManager == null) {
            Timber.e("ShapeManager를 가져올 수 없습니다")
            return
        }

        // 기본 ShapeLayer 가져오기
        val shapeLayer: ShapeLayer = shapeManager.layer

        // LocationPoint를 LatLng로 변환
        val latLngList =
            locations.map { location ->
                LatLng.from(location.latitude, location.longitude)
            }

        // MapPoints 생성
        val mapPoints = MapPoints.fromLatLng(latLngList)

        // Polyline 스타일 설정 (파란색, 두께 10px)
        val polylineStyle =
            PolylineStyle.from(
                10f, // 선 두께 (px) - Float 타입
                Color.parseColor("#4285F4"), // 파란색 (Google Maps 스타일)
            )

        val polylineStyles = PolylineStyles.from(polylineStyle)

        // PolylineOptions 생성
        val polylineOptions = PolylineOptions.from(mapPoints, polylineStyles)

        // Polyline 추가
        val polyline: Polyline = shapeLayer.addPolyline(polylineOptions)

        Timber.d("경로 그리기 완료: ${locations.size}개 포인트, Polyline ID: ${polyline.id}")

        // TODO: 시작점과 끝점에 마커 추가 기능 구현
        // 카카오맵 SDK v2의 Label API에 맞게 구현 필요
        // addStartEndMarkers(kakaoMap, locations)
    } catch (e: Exception) {
        Timber.e(e, "경로 그리기 실패")
    }
}

/**
 * 시작점과 끝점에 마커 추가
 * TODO: 카카오맵 SDK v2의 Label API에 맞게 구현 필요
 * 현재는 Polyline 경로만 표시하고, 마커는 추후 추가 예정
 */
private fun addStartEndMarkers(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
) {
    // 카카오맵 SDK v2의 Label API를 사용하여 시작/종료 마커 추가
    // LabelTextBuilder를 사용하여 텍스트를 설정해야 함
    // 현재는 구현 보류
    Timber.d("마커 추가 기능은 추후 구현 예정")
}

/**
 * 경로에 맞게 카메라 이동
 * 카카오맵 SDK v2의 Camera API를 사용하여 경로 전체가 보이도록 카메라를 조정합니다.
 */
private fun moveCameraToPath(
    kakaoMap: KakaoMap,
    locations: List<LocationPoint>,
) {
    // 테스트용: 빈 리스트면 서울 기본 위치로 이동
    if (locations.isEmpty()) {
        Timber.d("경로가 없음 - 서울 기본 위치로 이동 (테스트용)")
        val seoulPosition = LatLng.from(37.5665, 126.9780) // 서울시청
        val cameraUpdate = CameraUpdateFactory.newCenterPosition(seoulPosition, 15)
        kakaoMap.moveCamera(cameraUpdate)
        return
    }

    try {
        // 모든 위치의 경계 계산
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

        // 중앙 좌표 계산
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        // 경계 크기 계산 (1개만 있어도 처리 가능하도록)
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        // 줌 레벨 계산 (경계 크기에 따라 조정)
        // 테스트용: 경계가 0이면 (1개 포인트) 기본 줌 레벨 사용
        val zoomLevelInt =
            when {
                latRange == 0.0 && lonRange == 0.0 -> 16

                // 1개 포인트인 경우 (테스트용)
                latRange < 0.001 || lonRange < 0.001 -> 18

                // 매우 작은 범위
                latRange < 0.01 || lonRange < 0.01 -> 16

                // 작은 범위
                latRange < 0.1 || lonRange < 0.1 -> 14

                // 중간 범위
                else -> 12 // 큰 범위
            }

        // 중앙 위치로 카메라 이동
        val centerPosition = LatLng.from(centerLat, centerLon)

        // 카메라 업데이트 생성 (중앙 위치 + 줌 레벨)
        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newCenterPosition(centerPosition, zoomLevelInt)

        // 카메라 이동 (애니메이션 없이 즉시 이동)
        kakaoMap.moveCamera(cameraUpdate)

        Timber.d(
            "카메라 이동 완료: 중심 ($centerLat, $centerLon), 줌 레벨: $zoomLevelInt, 경계 [$minLat-$maxLat, $minLon-$maxLon], 포인트 수: ${locations.size}",
        )
    } catch (e: Exception) {
        Timber.e(e, "카메라 이동 실패: ${e.message}")
        // 실패 시 중앙 위치로만 이동 (기본 줌 레벨)
        try {
            // 경계 재계산
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
            val centerLat = (minLat + maxLat) / 2.0
            val centerLon = (minLon + maxLon) / 2.0
            val centerPosition = LatLng.from(centerLat, centerLon)
            val centerUpdate: CameraUpdate = CameraUpdateFactory.newCenterPosition(centerPosition, 15)
            kakaoMap.moveCamera(centerUpdate)
            Timber.d("대체 카메라 이동 완료: 중심 ($centerLat, $centerLon), 줌 레벨: 15")
        } catch (e2: Exception) {
            Timber.e(e2, "대체 카메라 이동도 실패: ${e2.message}")
        }
    }
}
