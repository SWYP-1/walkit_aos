package team.swyp.sdu.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.utils.LocationTestData
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
        fun setMapViewSize(width: Int, height: Int) {
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
                                mapViewHeight
                            )
                            
                            // UI 상태 업데이트 (카메라 설정만 변경)
                            val currentUiState = _uiState.value
                            if (currentUiState is KakaoMapUiState.Ready) {
                                _uiState.value = currentUiState.copy(
                                    cameraSettings = cameraSettings
                                )
                                Timber.d("화면 크기 변경으로 인한 카메라 설정 재계산 완료")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "화면 크기 변경 시 카메라 설정 재계산 실패")
                        }
                    }
                }
            }
        }

        /**
         * 경로 설정 및 지도 업데이트 요청
         */
        fun setLocations(locations: List<LocationPoint>) {
            viewModelScope.launch {
                try {
                    // locations 변경 감지
                    val locationsChanged =
                        previousLocations.size != locations.size ||
                            previousLocations.firstOrNull()?.latitude != locations.firstOrNull()?.latitude ||
                            previousLocations.lastOrNull()?.latitude != locations.lastOrNull()?.latitude

                    if (!locationsChanged && currentLocations.isNotEmpty()) {
                        return@launch // 변경사항 없음
                    }

                    // 사용할 경로 데이터 결정
                    val locationsToUse = prepareLocations(locations)
                    currentLocations = locationsToUse
                    previousLocations = locations.toList()

                    // 새로운 경로 설정 시 렌더링 상태 초기화
                    _renderState.value = MapRenderState.Idle

                    // 카메라 설정 계산 (화면 크기 고려)
                    val cameraSettings = calculateCameraSettings(
                        locationsToUse,
                        mapViewWidth,
                        mapViewHeight
                    )

                    // UI 상태 업데이트
                    _uiState.value = KakaoMapUiState.Ready(
                        locations = locationsToUse,
                        cameraSettings = cameraSettings,
                        shouldDrawPath = locationsToUse.size >= 2,
                    )

                    Timber.d("경로 설정 완료: ${locationsToUse.size}개 포인트")
                } catch (e: Exception) {
                    Timber.e(e, "경로 설정 실패")
                    _uiState.value = KakaoMapUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다")
                }
            }
        }

        /**
         * 경로 데이터 준비 (테스트 데이터 처리 포함)
         */
        private fun prepareLocations(locations: List<LocationPoint>): List<LocationPoint> {
            return if (locations.size <= 1) {
                Timber.d("위치 포인트가 ${locations.size}개 - 테스트용 하드코딩 위치(용인) 사용")
                LocationTestData.getYonginTestLocations()
            } else {
                Timber.d("위치 포인트가 ${locations.size}개 - 원래 값 사용")
                locations
            }
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
            mapViewHeight: Int = 0
        ): CameraSettings {
            if (locations.isEmpty()) {
                // 기본 서울 위치
                return CameraSettings(
                    centerLat = 37.5665,
                    centerLon = 126.9780,
                    zoomLevel = 15,
                )
            }

            // 경계 계산
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

            // 경계 범위 계산
            val latRange = maxLat - minLat
            val lonRange = maxLon - minLon

            // 패딩 추가 (경로가 화면 가장자리에 붙지 않도록 여유 공간 추가)
            // 경로가 잘리지 않도록 충분한 여유 공간 확보 (더 보수적으로 설정)
            val paddingFactor = 2.0 // 기존 1.5에서 증가하여 더 넓은 여유 공간 확보
            val paddedLatRange = latRange * paddingFactor
            val paddedLonRange = lonRange * paddingFactor

            // 줌 레벨 계산 (실제 화면 크기 고려)
            val zoomLevel = calculateZoomLevel(
                latRange = paddedLatRange,
                lonRange = paddedLonRange,
                centerLat = centerLat,
                mapViewWidth = mapViewWidth,
                mapViewHeight = mapViewHeight,
            )

            Timber.d("카메라 설정 계산: 경계 범위 (lat: $latRange, lon: $lonRange), " +
                    "패딩 적용 후 (lat: $paddedLatRange, lon: $paddedLonRange), 줌 레벨: $zoomLevel")

            return CameraSettings(
                centerLat = centerLat,
                centerLon = centerLon,
                zoomLevel = zoomLevel,
                minLat = minLat,
                maxLat = maxLat,
                minLon = minLon,
                maxLon = maxLon,
            )
        }

        /**
         * 경계 범위를 기반으로 적절한 줌 레벨 계산
         * 위도와 경도 범위를 고려하여 경로가 화면에 잘 보이도록 줌 레벨을 결정합니다.
         * 
         * @param latRange 위도 범위 (도 단위)
         * @param lonRange 경도 범위 (도 단위)
         * @param centerLat 중심 위도 (경도 변환 계산용)
         * @param mapViewWidth MapView 너비 (픽셀, 0이면 기본값 사용)
         * @param mapViewHeight MapView 높이 (픽셀, 0이면 기본값 사용)
         */
        private fun calculateZoomLevel(
            latRange: Double,
            lonRange: Double,
            centerLat: Double,
            mapViewWidth: Int = 0,
            mapViewHeight: Int = 0,
        ): Int {
            // 위도 1도 ≈ 111km
            // 경도 1도 ≈ 111km * cos(위도)
            val latDegreeToKm = 111.0
            val lonDegreeToKm = 111.0 * kotlin.math.cos(kotlin.math.PI * centerLat / 180.0)

            // 경계 범위를 km로 변환
            val latRangeKm = latRange * latDegreeToKm
            val lonRangeKm = lonRange * lonDegreeToKm

            // 실제 화면 크기를 기반으로 화면 비율 계산
            val screenAspectRatio = if (mapViewWidth > 0 && mapViewHeight > 0) {
                mapViewWidth.toDouble() / mapViewHeight.toDouble()
            } else {
                // 화면 크기가 아직 측정되지 않았으면 기본값 사용 (16:9 비율)
                1.78
            }
            
            // 화면 비율을 고려하여 각 축의 필요한 범위 계산
            // 가로가 더 넓으면 경도 범위를 더 크게 고려해야 하고,
            // 세로가 더 넓으면 위도 범위를 더 크게 고려해야 함
            val adjustedLatRangeKm = if (screenAspectRatio < 1.0) {
                // 세로가 더 긴 경우 (세로 모드 등)
                latRangeKm / screenAspectRatio
            } else {
                latRangeKm
            }
            
            val adjustedLonRangeKm = if (screenAspectRatio > 1.0) {
                // 가로가 더 긴 경우 (가로 모드 등)
                lonRangeKm * screenAspectRatio
            } else {
                lonRangeKm
            }
            
            // 위도와 경도 범위 중 더 큰 값을 기준으로 줌 레벨 계산
            // 더 보수적으로 계산하여 경로가 잘리지 않도록 함
            val maxRangeKm = maxOf(adjustedLatRangeKm, adjustedLonRangeKm)
            
            Timber.d("줌 레벨 계산: 화면 크기=${mapViewWidth}x${mapViewHeight}, " +
                    "화면 비율=$screenAspectRatio, " +
                    "위도 범위=${latRangeKm}km, 경도 범위=${lonRangeKm}km, " +
                    "조정된 위도 범위=${adjustedLatRangeKm}km, 조정된 경도 범위=${adjustedLonRangeKm}km, " +
                    "최대 범위=${maxRangeKm}km")

            // 줌 레벨 계산 공식 (카카오맵 기준)
            // 줌 레벨 1: 약 200km
            // 줌 레벨 2: 약 100km
            // 줌 레벨 3: 약 50km
            // ... (각 줌 레벨마다 약 2배씩 확대)
            // 줌 레벨 n: 약 200 / (2^(n-1)) km
            
            // 줌 레벨 계산 (더 보수적으로 - 한 단계 더 줌아웃하여 경로가 잘리지 않도록)
            // 각 범위의 상한값을 사용하여 더 넓은 범위를 보여주도록 함
            val zoomLevel = when {
                maxRangeKm <= 0.0 -> 18 // 단일 포인트
                maxRangeKm <= 0.1 -> 17 // 약 100m (기존 18 -> 17로 한 단계 줌아웃)
                maxRangeKm <= 0.5 -> 16 // 약 500m (기존 17 -> 16)
                maxRangeKm <= 1.0 -> 15 // 약 1km (기존 16 -> 15)
                maxRangeKm <= 2.0 -> 14 // 약 2km (기존 15 -> 14)
                maxRangeKm <= 5.0 -> 13 // 약 5km (기존 14 -> 13)
                maxRangeKm <= 10.0 -> 12 // 약 10km (기존 13 -> 12)
                maxRangeKm <= 20.0 -> 11 // 약 20km (기존 12 -> 11)
                maxRangeKm <= 50.0 -> 10 // 약 50km (기존 11 -> 10)
                maxRangeKm <= 100.0 -> 9 // 약 100km (기존 10 -> 9)
                maxRangeKm <= 200.0 -> 8 // 약 200km (기존 9 -> 8)
                else -> 8 // 그 이상
            }

            // 최소/최대 줌 레벨 제한
            return zoomLevel.coerceIn(8, 18)
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
            if (_renderState.value == MapRenderState.DrawingPath) {
                _renderState.value = MapRenderState.Complete
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

