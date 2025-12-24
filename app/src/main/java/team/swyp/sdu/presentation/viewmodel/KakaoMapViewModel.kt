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

                    // 카메라 설정 계산
                    val cameraSettings = calculateCameraSettings(locationsToUse)

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
         */
        private fun calculateCameraSettings(locations: List<LocationPoint>): CameraSettings {
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

            // 패딩 추가 (경로가 화면 가장자리에 붙지 않도록 약 24dp에 해당하는 여유 공간 추가)
            // 화면 크기에 비례하여 약 15-20% 정도의 패딩을 추가하면 24dp 정도의 여유 공간 확보
            val paddingFactor = 1.35 // 기존 1.2에서 증가하여 더 넓은 여유 공간 확보
            val paddedLatRange = latRange * paddingFactor
            val paddedLonRange = lonRange * paddingFactor

            // 줌 레벨 계산 (더 정확한 알고리즘)
            val zoomLevel = calculateZoomLevel(
                latRange = paddedLatRange,
                lonRange = paddedLonRange,
                centerLat = centerLat,
            )

            Timber.d("카메라 설정 계산: 경계 범위 (lat: $latRange, lon: $lonRange), 줌 레벨: $zoomLevel")

            return CameraSettings(
                centerLat = centerLat,
                centerLon = centerLon,
                zoomLevel = zoomLevel,
            )
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
            // 줌 레벨 1: 약 200km
            // 줌 레벨 2: 약 100km
            // 줌 레벨 3: 약 50km
            // ... (각 줌 레벨마다 약 2배씩 확대)
            // 줌 레벨 n: 약 200 / (2^(n-1)) km
            
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

