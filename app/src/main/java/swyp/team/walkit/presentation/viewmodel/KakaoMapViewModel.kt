package swyp.team.walkit.presentation.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import swyp.team.walkit.data.model.LocationPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 카카오맵 ViewModel
 * 지도 관련 비즈니스 로직과 상태를 관리합니다.
 */
@HiltViewModel
class KakaoMapViewModel
@Inject
constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<KakaoMapUiState>(KakaoMapUiState.Initial)
    val uiState: StateFlow<KakaoMapUiState> = _uiState.asStateFlow()

    // 강제 업데이트용 카운터
    private var updateCounter = 0

    private val _snapshotState = MutableStateFlow<Bitmap?>(null)
    val snapshotState: StateFlow<Bitmap?> = _snapshotState.asStateFlow()

    // 렌더링 상태 머신
    private val _renderState = MutableStateFlow<MapRenderState>(MapRenderState.Idle)
    val renderState: StateFlow<MapRenderState> = _renderState.asStateFlow()

    // 현재 경로 데이터
    private var currentLocations: List<LocationPoint> = emptyList()
    private var previousLocations: List<LocationPoint> = emptyList()

    // MapView 화면 크기 (픽셀 단위)
    private var mapViewWidth: Int = 0
    private var mapViewHeight: Int = 0

    /**
     * MapView 화면 크기 설정
     * 크기가 변경되면 현재 경로에 대해 카메라 설정을 재계산합니다.
     */
    fun setMapViewSize(width: Int, height: Int, localDensity: Density) {

        if (width > 0 && height > 0 && (mapViewWidth != width || mapViewHeight != height)) {
            mapViewWidth = width
            mapViewHeight = height
            Timber.d("MapView 크기 설정: ${width}x${height}")

            // 현재 경로가 있으면 카메라 설정 재계산
            if (currentLocations.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        val cameraSettings = calculateCameraSettings(
                            currentLocations,
                            mapViewWidth,
                            mapViewHeight,
                            localDensity
                        )

                        // UI 상태 업데이트 (카메라 설정만 변경)
                        val currentUiState = _uiState.value
                        if (currentUiState is KakaoMapUiState.Ready) {
                            _uiState.value = currentUiState.copy(
                                cameraSettings = cameraSettings
                            )
                            Timber.d("화면 크기 변경으로 인한 카메라 설정 재계산 완료")
                        }
                    } catch (t: Throwable) {
                        Timber.e(t, "화면 크기 변경 시 카메라 설정 재계산 실패")
                    }
                }
            }
        }
    }

    /**
     * 경로 초기화 (모든 경로 라인 제거)
     */
    fun clearPaths() {
        // 현재 UI 상태에서 경로를 비활성화
        val currentUiState = _uiState.value
        if (currentUiState is KakaoMapUiState.Ready) {
            _uiState.value = currentUiState.copy(shouldDrawPath = false)
            Timber.d("경로 초기화 완료")
        }
    }

    /**
     * 줌 레벨 수동 설정 (ZoomLevelTestScreen용)
     */
    fun setZoomLevel(zoomLevel: Int) {
        val coercedZoomLevel = zoomLevel.coerceIn(8, 18)

        // 현재 UI 상태가 Ready인 경우 카메라 설정 업데이트
        val currentUiState = _uiState.value
        if (currentUiState is KakaoMapUiState.Ready) {
            val updatedCameraSettings = currentUiState.cameraSettings.copy(zoomLevel = coercedZoomLevel)
            _uiState.value = currentUiState.copy(cameraSettings = updatedCameraSettings)

            Timber.d("줌 레벨 수동 설정: $coercedZoomLevel")
        }
    }

    /**
     * 카메라 업데이트 강제 실행 (줌 레벨 변경용)
     */
    fun forceUpdateCamera() {
        val currentUiState = _uiState.value
        if (currentUiState is KakaoMapUiState.Ready) {
            // UI 상태를 새로운 객체로 설정해서 updateMapFromState가 트리거되도록 함
            _uiState.value = currentUiState.copy() // copy()로 새로운 객체 생성
            Timber.d("카메라 업데이트 강제 실행")
        }
    }

    /**
     * 경로 설정 및 지도 업데이트 요청
     */
    fun setLocations(locations: List<LocationPoint>,localDensity : Density) {
        Timber.d("📍 KakaoMapViewModel.setLocations 호출 - ${locations.size}개 포인트")
        viewModelScope.launch {
            try {
                // locations 변경 감지 - 더 강력한 검증
                val locationsChanged = previousLocations.size != locations.size ||
                    previousLocations.isEmpty() ||
                    currentLocations.isEmpty() ||
                    previousLocations.firstOrNull()?.latitude != locations.firstOrNull()?.latitude ||
                    previousLocations.lastOrNull()?.latitude != locations.lastOrNull()?.latitude

                Timber.d("📍 locationsChanged=$locationsChanged, previous=${previousLocations.size}, current=${locations.size}, firstLat=${locations.firstOrNull()?.latitude}")

                // 항상 처음 호출 때는 처리 (currentLocations이 비어있을 때)
                val shouldProcess = locationsChanged || currentLocations.isEmpty()
                if (!shouldProcess) {
                    Timber.d("📍 변경사항 없음 - 리턴")
                    return@launch
                }

                // 사용할 경로 데이터 결정
                val locationsToUse = prepareLocations(locations)
                currentLocations = locationsToUse
                previousLocations = locations.toList()

                Timber.d("📍 사용할 위치 데이터: ${locationsToUse.size}개 포인트")

                // 새로운 경로 설정 시 렌더링 상태 초기화
                _renderState.value = MapRenderState.Idle
                Timber.d("📍 renderState를 Idle로 초기화")

                // 카메라 설정 계산 (화면 크기 고려)
                val cameraSettings = calculateCameraSettings(
                    locationsToUse,
                    mapViewWidth,
                    mapViewHeight,
                    localDensity
                )

                Timber.d("📍 카메라 설정 계산 완료: center=(${cameraSettings.centerLat}, ${cameraSettings.centerLon}), zoom=${cameraSettings.zoomLevel}")

                // UI 상태 업데이트
                _uiState.value = KakaoMapUiState.Ready(
                    locations = locationsToUse,
                    cameraSettings = cameraSettings,
                    shouldDrawPath = locationsToUse.size >= 2,
                )

                Timber.d("✅ 경로 설정 완료: ${locationsToUse.size}개 포인트, shouldDrawPath=${locationsToUse.size >= 2}")
            } catch (t: Throwable) {
                Timber.e(t, "경로 설정 실패")
                _uiState.value = KakaoMapUiState.Error(t.message ?: "알 수 없는 오류가 발생했습니다")
            }
        }
    }

    /**
     * 경로 데이터 준비 (테스트 데이터 처리 포함)
     */
    private fun prepareLocations(locations: List<LocationPoint>): List<LocationPoint> {
//        return if (locations.size <= 1) {
//            Timber.d("위치 포인트가 ${locations.size}개 - 테스트용 하드코딩 위치(용인) 사용")
//            generateRandomCityWalkPoints()
//        } else {
//
//        }
        return locations
    }

    /**
     * 카메라 설정 계산
     * 경로의 경계를 기반으로 적절한 줌 레벨을 계산합니다.
     *
     * @param locations 경로 위치 포인트 리스트
     * @param mapViewWidth MapView 너비 (픽셀, 0이면 기본값 사용)
     * @param mapViewHeight MapView 높이 (픽셀, 0이면 기본값 사용)
     */
    private fun calculateCameraSettings(
        locations: List<LocationPoint>,
        mapViewWidth: Int = 0,
        mapViewHeight: Int = 0,
        localDensity: Density,
        paddingDp: Int = 32 // 패딩 파라미터 추가 (기본값 32dp)
    ): CameraSettings {
        if (locations.isEmpty()) {
            return CameraSettings(
                centerLat = 37.5665,
                centerLon = 126.9780,
                zoomLevel = 12
            )
        }

        if (locations.size == 1) {
            return CameraSettings(
                centerLat = locations[0].latitude,
                centerLon = locations[0].longitude,
                zoomLevel = 16
            )
        }

        // 위도/경도 범위 계산
        val latitudes = locations.map { it.latitude }
        val longitudes = locations.map { it.longitude }

        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 0.0

        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        // 중심점 계산
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        // 줌 레벨 계산
        val zoomLevel = calculateZoomLevel(
            latRange = latRange,
            lonRange = lonRange,
            centerLat = centerLat,
            mapViewWidth = mapViewWidth,
            mapViewHeight = mapViewHeight,
            localDensity = localDensity,
            paddingDp = paddingDp
        )

        return CameraSettings(
            centerLat = centerLat,
            centerLon = centerLon,
            zoomLevel = zoomLevel,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon
        )
    }

    private fun calculateZoomLevel(
        latRange: Double,
        lonRange: Double,
        centerLat: Double,
        mapViewWidth: Int = 0,
        mapViewHeight: Int = 0,
        localDensity: Density,
        paddingDp: Int = 32 // 패딩 파라미터 추가 (기본값 32dp)
    ): Int {
        // 패딩 값 (dp를 px로 변환) - 경로가 테두리에 걸리지 않도록
        val paddingPx = with(localDensity) { paddingDp.dp.toPx() }.toInt()

        // 패딩을 고려한 실제 사용 가능한 화면 크기
        val usableWidth = (mapViewWidth - paddingPx * 2).coerceAtLeast(1)
        val usableHeight = (mapViewHeight - paddingPx * 2).coerceAtLeast(1)

        val latDegreeToKm = 111.0
        val lonDegreeToKm = 111.0 * kotlin.math.cos(kotlin.math.PI * centerLat / 180.0)

        val latRangeKm = latRange * latDegreeToKm
        val lonRangeKm = lonRange * lonDegreeToKm

        val screenAspectRatio = if (usableWidth > 0 && usableHeight > 0) {
            usableWidth.toDouble() / usableHeight.toDouble()
        } else {
            1.78
        }

        val routeAspectRatio = if (latRangeKm > 0 && lonRangeKm > 0) {
            lonRangeKm / latRangeKm
        } else {
            screenAspectRatio
        }

        // 어느 방향이 제약 조건인지 판단
        val maxRangeKm = if (routeAspectRatio > screenAspectRatio) {
            // 경로가 더 가로로 넓음 -> 경도가 꽉 차도록
            lonRangeKm / screenAspectRatio
        } else {
            // 경로가 더 세로로 길음 -> 위도가 꽉 차도록
            latRangeKm * screenAspectRatio
        }

        // 더 보수적인 패딩 적용 (약 25-30% 여유 공간)
        val paddingFactor = 1.25
        val adjustedMaxRangeKm = maxRangeKm * paddingFactor

        Timber.d(
            "줌 레벨 계산: " +
                    "원본 화면=${mapViewWidth}x${mapViewHeight}, " +
                    "패딩=${paddingPx}px, " +
                    "사용 가능 화면=${usableWidth}x${usableHeight}, " +
                    "화면 비율=$screenAspectRatio, " +
                    "경로 비율=$routeAspectRatio, " +
                    "위도 범위=${latRangeKm}km, 경도 범위=${lonRangeKm}km, " +
                    "최대 범위=${maxRangeKm}km, " +
                    "패딩 적용 후=${adjustedMaxRangeKm}km"
        )

        // 줌 레벨 계산 (더 보수적으로 - 경로가 테두리에 걸리지 않도록)
        val zoomLevel = when {
            adjustedMaxRangeKm <= 0.0 -> 17  // 더 축소
            adjustedMaxRangeKm <= 0.1 -> 17   // 약 100m
            adjustedMaxRangeKm <= 0.5 -> 16   // 약 500m
            adjustedMaxRangeKm <= 1.0 -> 15   // 약 1km
            adjustedMaxRangeKm <= 2.0 -> 14   // 약 2km
            adjustedMaxRangeKm <= 5.0 -> 13   // 약 5km
            adjustedMaxRangeKm <= 10.0 -> 12  // 약 10km
            adjustedMaxRangeKm <= 20.0 -> 11  // 약 20km
            adjustedMaxRangeKm <= 50.0 -> 10  // 약 50km
            adjustedMaxRangeKm <= 100.0 -> 9  // 약 100km
            adjustedMaxRangeKm <= 200.0 -> 8  // 약 200km
            else -> 7  // 더 축소
        }.coerceIn(7, 18)

        return zoomLevel
    }

    /**
     * 카메라 이동 시작
     */
    fun startCameraMove() {
        _renderState.value = MapRenderState.MovingCamera
    }

    /**
     * 카메라 이동 완료
     */
    fun onCameraMoveComplete() {
        if (_renderState.value == MapRenderState.MovingCamera) {
            _renderState.value = MapRenderState.DrawingPath
        }
    }

    /**
     * 경로 그리기 완료
     */
    fun onPathDrawComplete() {
        Timber.d("🔄 onPathDrawComplete 호출 - 현재 상태: ${_renderState.value}")
        if (_renderState.value == MapRenderState.DrawingPath) {
            _renderState.value = MapRenderState.Complete
            Timber.d("✅ renderState를 Complete로 변경")
        } else {
            Timber.w("⚠️ onPathDrawComplete: 현재 상태가 DrawingPath가 아님 - ${_renderState.value}")
        }
    }

    /**
     * 스냅샷 저장
     */
    fun setSnapshot(bitmap: Bitmap?) {
        _snapshotState.value = bitmap
    }

    /**
     * 상태 초기화
     */
    fun reset() {
        _uiState.value = KakaoMapUiState.Initial
        _snapshotState.value = null
        _renderState.value = MapRenderState.Idle
        currentLocations = emptyList()
        previousLocations = emptyList()
    }
}

/**
 * 카카오맵 UI 상태
 */
sealed interface KakaoMapUiState {
    /**
     * 초기 상태
     */
    data object Initial : KakaoMapUiState

    /**
     * 지도 준비 완료
     */
    data class Ready(
        val locations: List<LocationPoint>,
        val cameraSettings: CameraSettings,
        val shouldDrawPath: Boolean,
    ) : KakaoMapUiState

    /**
     * 오류 상태
     */
    data class Error(
        val message: String,
    ) : KakaoMapUiState
}

/**
 * 카메라 설정
 */
data class CameraSettings(
    val centerLat: Double,
    val centerLon: Double,
    val zoomLevel: Int,
    val minLat: Double? = null,
    val maxLat: Double? = null,
    val minLon: Double? = null,
    val maxLon: Double? = null,
)

/**
 * 지도 렌더링 상태 머신
 */
sealed class MapRenderState {
    data object Idle : MapRenderState()
    data object MovingCamera : MapRenderState()
    data object DrawingPath : MapRenderState()
    data object Complete : MapRenderState() // Ready → Complete (자동 스냅샷 없음)
}

