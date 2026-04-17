package swyp.team.walkit.ui.interactivemap

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import swyp.team.walkit.R
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.model.MapMarker
import swyp.team.walkit.data.model.MapMarkerType
import swyp.team.walkit.data.model.toMapMarker
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.FollowerMapRecord
import swyp.team.walkit.domain.model.FollowerRecentActivity
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.domain.model.NearbySpot
import swyp.team.walkit.domain.model.WalkPoint
import swyp.team.walkit.domain.model.toMapMarker
import swyp.team.walkit.domain.repository.FollowerMapRepository
import swyp.team.walkit.domain.repository.RecentSearchRepository
import swyp.team.walkit.domain.repository.SpotRepository
import swyp.team.walkit.domain.repository.WalkRepository
import swyp.team.walkit.domain.service.LocationManager
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.utils.LocationTestData
import timber.log.Timber
import javax.inject.Inject

private const val LIKE_DEBOUNCE_MS = 500L

/**
 * 지도 위치 추적 모드
 *
 * IDLE      → 추적 안 함 (기본)
 * FOLLOWING → 현재 위치 추적 중 (지도가 내 위치를 따라감)
 */
enum class MapTrackingMode { IDLE, FOLLOWING }

/**
 * 친구 상세 화면 이동 이벤트
 */
data class FriendDetailNavEvent(val userId: Long, val walkId: Long)

/**
 * 스팟 바텀시트 내용 상태
 */
sealed interface SpotSheetContent {
    /** 기본 상태 — 주변 추천 스팟 2열 그리드 */
    data object SpotList : SpotSheetContent
    /** 최근 검색어 목록 */
    data object RecentSearch : SpotSheetContent
    /** 검색바 + 검색 결과 2열 그리드 */
    data object Search : SpotSheetContent
    /** 스팟 상세 (RecommendSpotPinBottomTab) */
    data class SpotDetail(val spot: NearbySpot) : SpotSheetContent
}

/**
 * 스팟 바텀시트 단방향 이벤트
 */
sealed interface SpotSheetEvent {
    /** 시트를 전체 펼침 상태로 확장 (애니메이션) */
    data object ExpandSheet : SpotSheetEvent
    /** 시트를 전체 펼침 상태로 즉시 snap (애니메이션 없음 — SpotList 노출 방지) */
    data object SnapToExpand : SpotSheetEvent
    /** 시트를 중간(partialExpand) 상태로 축소 */
    data object PartialExpand : SpotSheetEvent
}

/**
 * 인터랙티브 지도 화면 UI 상태
 *
 * @property spots               서버에서 받아온 주변 장소 목록
 * @property followerRecords     반경 내 팔로워 산책 기록 마커 목록
 * @property recentActivities    팔로우 목록 (최근 산책 순)
 * @property spotSheetContent    스팟 바텀시트 내용 상태
 * @property spotSearchQuery     스팟 검색어
 * @property spotSearchResults   스팟 검색 결과
 * @property isSearchingSpots    스팟 검색 로딩 중 여부
 * @property recentSearchQueries 최근 검색어 목록 (최신순, 최대 10개)
 * @property errorMessage        에러 메시지 (null이면 에러 없음)
 */
data class InteractiveMapUiState(
    val spots: List<NearbySpot> = emptyList(),
    val followerRecords: List<FollowerMapRecord> = emptyList(),
    val recentActivities: List<FollowerRecentActivity> = emptyList(),
    val spotSheetContent: SpotSheetContent = SpotSheetContent.SpotList,
    val spotSearchQuery: String = "",
    val spotSearchResults: List<NearbySpot> = emptyList(),
    val isSearchingSpots: Boolean = false,
    val recentSearchQueries: List<String> = emptyList(),
    /** userId → 처리 완료된 Lottie JSON 문자열 (null이면 fallback 이니셜 표시) */
    val followerLottieJsonMap: Map<Long, String?> = emptyMap(),
    /** userId → 지도 핀용 Bitmap (null이면 ic_pin_friend 기본 아이콘 사용) */
    val followerPinBitmapMap: Map<Long, Bitmap?> = emptyMap(),
    val trackingMode: MapTrackingMode = MapTrackingMode.IDLE,
    /** FOLLOWING 모드일 때 폴링된 현재 위치 (null이면 아직 위치 없음) */
    val currentLocation: LocationPoint? = null,
    val errorMessage: String? = null,
) {
    /** 장소 마커 + 친구 마커를 합친 전체 마커 목록 */
    val allMarkers: List<MapMarker>
        get() = spots.map { it.toMapMarker() } + followerRecords.map { it.toMapMarker() }
}

/**
 * 인터랙티브 지도 화면 ViewModel
 *
 * [SpotRepository]와 [FollowerMapRepository]를 통해 주변 장소 및
 * 팔로워 산책 기록을 조회하고 UI 상태를 관리한다.
 */
@HiltViewModel
class InteractiveMapViewModel @Inject constructor(
    application: Application,
    private val spotRepository: SpotRepository,
    private val followerMapRepository: FollowerMapRepository,
    private val walkRepository: WalkRepository,
    private val locationManager: LocationManager,
    private val recentSearchRepository: RecentSearchRepository,
    private val lottieImageProcessor: LottieImageProcessor,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(InteractiveMapUiState())
    val uiState: StateFlow<InteractiveMapUiState> = _uiState.asStateFlow()

    /** 스팟 바텀시트 단방향 이벤트 (시트 확장 등) */
    private val _spotSheetEvents = MutableSharedFlow<SpotSheetEvent>(extraBufferCapacity = 1)
    val spotSheetEvents: SharedFlow<SpotSheetEvent> = _spotSheetEvents

    /** 친구 상세 화면 이동 이벤트 */
    private val _friendDetailNavEvent = MutableSharedFlow<FriendDetailNavEvent>(extraBufferCapacity = 1)
    val friendDetailNavEvent: SharedFlow<FriendDetailNavEvent> = _friendDetailNavEvent

    /** 좋아요 debounce 잡 */
    private var likeToggleJob: Job? = null

    /** FOLLOWING 모드일 때 현재 위치를 주기적으로 업데이트하는 잡 */
    private var locationPollingJob: Job? = null

    /** 세션 내 핀 Bitmap 캐시 — 지도 새로고침 시 재빌드 없이 재사용 */
    private val pinBitmapCache = HashMap<Long, Bitmap?>()

    /** 스팟 검색에 재사용할 현재 위치 */
    private var currentLat = SEOUL_CITY_HALL_LAT
    private var currentLon = SEOUL_CITY_HALL_LON

    init {
        // 최근 검색어를 Room Flow로 실시간 반영
        viewModelScope.launch {
            recentSearchRepository.getRecentSearches().collect { queries ->
                _uiState.update { it.copy(recentSearchQueries = queries) }
            }
        }
    }

    /**
     * 현재 기기 위치를 가져와 지도 데이터를 독립적으로 로드한다.
     *
     * 장소·팔로워 산책 기록·팔로워 최근 활동 3개 API를 병렬로 시작하되,
     * 각 결과가 도착하는 즉시 UI를 업데이트한다 — 가장 느린 API를 기다리지 않는다.
     *
     * 위치 조회 실패 시 서울 시청 좌표로 폴백한다.
     */
    fun loadMapDataFromCurrentLocation(
        query: String = "카페",
        radius: Int = 1000,
        size: Int = 15,
        sort: String = "distance",
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val location = locationManager.getCurrentLocationOrLast()
            val (lon, lat) = if (location != null) {
                location.longitude to location.latitude
            } else {
                SEOUL_CITY_HALL_LON to SEOUL_CITY_HALL_LAT
            }
            currentLat = lat
            currentLon = lon

            // ① 주변 장소 — 완료되면 즉시 반영
            launch {
                val result = spotRepository.getNearbySpots(
                    query = query, x = lon, y = lat,
                    radius = radius, size = size, sort = sort,
                )
                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> state.copy(spots = result.data)
                        is Result.Error -> state.copy(errorMessage = result.message)
                        else -> state
                    }
                }
            }

            // ② 팔로워 최근 활동 (아바타 행 + Lottie JSON) — 완료되면 즉시 반영
            launch {
                val result = followerMapRepository.getFollowerRecentActivities()
                if (result is Result.Success) {
                    val lottieMap = buildFollowerLottieMap(result.data)
                    _uiState.update { it.copy(recentActivities = result.data, followerLottieJsonMap = lottieMap) }
                    // ③ 팔로워 산책 기록이 이미 로드됐다면 핀 Bitmap도 즉시 재빌드
                    rebuildPinBitmapsIfReady()
                    // TODO: 실제 API 연동 후 제거 — activities 기반 가짜 친구 핀 주입
                    injectFakeFollowerPins(activities = result.data)
                } else if (result is Result.Error) {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }

            // ③ 팔로워 산책 기록 (지도 핀) — 완료되면 즉시 반영
            // 주의: 빈 리스트로 overwrite하면 ②에서 주입한 가짜 핀이 사라지므로
            //       실제 데이터가 있을 때만 followerRecords를 갱신한다.
            launch {
                val result = followerMapRepository.getFollowerWalkingRecords(lat = lat, lon = lon, radius = radius)
                if (result is Result.Success && result.data.isNotEmpty()) {
                    _uiState.update { it.copy(followerRecords = result.data) }
                    // ② Lottie JSON이 이미 준비됐다면 핀 Bitmap 바로 빌드
                    rebuildPinBitmapsIfReady()
                } else if (result is Result.Error) {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    /**
     * followerRecords와 followerLottieJsonMap이 모두 준비된 경우에만 핀 Bitmap을 빌드한다.
     *
     * ② ③ 중 어느 쪽이 먼저 끝나든 늦게 끝난 쪽이 이 함수를 호출하면 Bitmap을 생성한다.
     */
    private suspend fun rebuildPinBitmapsIfReady() {
        val state = _uiState.value
        // 둘 다 비어있지 않아야 의미있는 빌드
        if (state.followerRecords.isEmpty() || state.followerLottieJsonMap.isEmpty()) return
        val pinBitmapMap = buildFollowerPinBitmapMap(state.followerRecords, state.followerLottieJsonMap)
        _uiState.update { it.copy(followerPinBitmapMap = pinBitmapMap) }
    }

    /**
     * [FollowerRecentActivity] 목록을 기반으로 현재 위치 주변에 가짜 친구 핀을 주입한다.
     *
     * 실제 API(`getFollowerWalkingRecords`)가 없는 개발·테스트 환경에서 사용한다.
     * [activities]를 명시하면 해당 목록을, 생략하면 현재 UI 상태의 recentActivities를 사용한다.
     *
     * - [loadMapDataFromCurrentLocation]의 ② 블록에서 자동 호출되므로
     *   화면 진입 시 별도 호출 없이도 가짜 핀이 표시된다.
     * - 외부에서 직접 호출해 수동으로 재주입할 수도 있다.
     *
     * @param activities recentActivities (null이면 현재 상태에서 읽음)
     * @param radiusKm   핀 배치 최대 반경 (기본 1.0 km)
     */
    fun injectFakeFollowerPins(
        activities: List<FollowerRecentActivity>? = null,
        radiusKm: Double = 1.0,
    ) {
        val targetActivities = activities ?: _uiState.value.recentActivities
        if (targetActivities.isEmpty()) {
            Timber.w("injectFakeFollowerPins: activities가 비어있음")
            return
        }
        val fakeRecords = LocationTestData.generateFakeFollowerMapRecords(
            activities = targetActivities,
            centerLat = currentLat,
            centerLon = currentLon,
            radiusKm = radiusKm,
        )
        _uiState.update { it.copy(followerRecords = fakeRecords) }
        Timber.d("가짜 친구 핀 ${fakeRecords.size}개 주입 완료 (반경 ${radiusKm}km) — allMarkers=${_uiState.value.allMarkers.size}개, lottieJsonMap=${_uiState.value.followerLottieJsonMap.size}개")
        // Lottie JSON이 이미 준비된 경우 핀 Bitmap 즉시 빌드
        viewModelScope.launch { rebuildPinBitmapsIfReady() }
    }

    /**
     * 마커 클릭 시 핀 타입을 구분한다.
     *
     * - 친구 핀: 친구 바텀시트 표시 + 최근 산책 기록 비동기 로드
     * - 스팟 핀: 스팟 검색 상태로 전환 + 시트 확장
     */
    fun onMarkerClick(marker: MapMarker) {
        when (marker.type) {
            MapMarkerType.FRIEND -> {
                val record = _uiState.value.followerRecords
                    .find { it.toMapMarker().id == marker.id } ?: return
                _friendDetailNavEvent.tryEmit(FriendDetailNavEvent(record.userId, record.walkId))
            }

            MapMarkerType.SPOT -> {
                // 마커 id(좌표 조합)로 spots 목록에서 해당 NearbySpot을 찾아 상세로 바로 이동
                val spot = _uiState.value.spots.find { it.toMapMarker().id == marker.id }
                if (spot != null) {
                    _uiState.update { it.copy(spotSheetContent = SpotSheetContent.SpotDetail(spot)) }
                    _spotSheetEvents.tryEmit(SpotSheetEvent.ExpandSheet)
                }
            }
        }
    }

    // ─── 스팟 바텀시트 이벤트 ────────────────────────────────────────────────

    /** 돋보기 아이콘 클릭 — 최근 검색어 목록 상태로 전환 + 시트를 즉시 snap (SpotList 노출 없음) */
    fun onSpotSearchIconClick() {
        _uiState.update { it.copy(spotSheetContent = SpotSheetContent.RecentSearch) }
        _spotSheetEvents.tryEmit(SpotSheetEvent.SnapToExpand)
    }

    /** 검색어 변경 */
    fun onSpotSearchQueryChange(query: String) {
        _uiState.update { it.copy(spotSearchQuery = query) }
    }

    /**
     * 검색 실행 — 현재 위치 기반 카카오 장소 검색
     *
     * 검색어를 Room에 저장하고, 결과 로드 후 시트를 중간 크기로 축소한다.
     */
    fun onSpotSearch() {
        val query = _uiState.value.spotSearchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            // 검색어 영속 저장
            recentSearchRepository.saveSearch(query)

            _uiState.update {
                it.copy(
                    isSearchingSpots = true,
                    spotSheetContent = SpotSheetContent.Search,
                )
            }

            val result = withContext(Dispatchers.IO) {
                spotRepository.getNearbySpots(
                    query = query,
                    x = currentLon,
                    y = currentLat,
                    radius = 5000,
                    size = 15,
                    sort = "accuracy",
                )
            }
            val results = if (result is Result.Success) result.data else emptyList()
            _uiState.update {
                it.copy(
                    isSearchingSpots = false,
                    spotSearchResults = results,
                )
            }
            // 결과가 있으면 시트를 확장해 검색 리스트가 화면 중간에 걸치도록 함
            if (results.isNotEmpty()) {
                _spotSheetEvents.tryEmit(SpotSheetEvent.ExpandSheet)
            }
        }
    }

    /** 최근 검색어 항목 클릭 — 해당 쿼리로 검색 실행 */
    fun onRecentSearchClick(query: String) {
        _uiState.update { it.copy(spotSearchQuery = query) }
        onSpotSearch()
    }

    /** 최근 검색어 개별 삭제 */
    fun onRemoveRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchRepository.deleteSearch(query)
        }
    }

    /** 최근 검색어 전체 삭제 */
    fun onClearAllRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearAll()
        }
    }

    /** 검색어 초기화 */
    fun onSpotSearchClear() {
        _uiState.update { it.copy(spotSearchQuery = "", spotSearchResults = emptyList()) }
    }

    /** 검색 결과 아이템 클릭 — SpotDetail 상태로 전환 */
    fun onSpotGridItemClick(spot: NearbySpot) {
        _uiState.update { it.copy(spotSheetContent = SpotSheetContent.SpotDetail(spot)) }
    }

    /** SpotDetail X 버튼 — SpotList로 복귀 */
    fun onSpotDetailClose() {
        _uiState.update {
            it.copy(
                spotSheetContent = SpotSheetContent.SpotList,
                spotSearchQuery = "",
                spotSearchResults = emptyList(),
            )
        }
    }

    /** 검색 뒤로가기 — RecentSearch로 복귀 */
    fun onSpotSearchBack() {
        _uiState.update {
            it.copy(
                spotSheetContent = SpotSheetContent.RecentSearch,
                spotSearchQuery = "",
                spotSearchResults = emptyList(),
            )
        }
    }

    /** 최근검색 뒤로가기 — SpotList로 복귀 */
    fun onRecentSearchBack() {
        _uiState.update {
            it.copy(
                spotSheetContent = SpotSheetContent.SpotList,
                spotSearchQuery = "",
            )
        }
    }

    /**
     * 트래킹 버튼 클릭 — IDLE ↔ FOLLOWING 토글
     *
     * FOLLOWING 전환 시 위치 폴링을 시작하고,
     * IDLE 전환 시 폴링을 중단하며 currentLocation을 초기화한다.
     */
    fun onTrackingButtonClick() {
        val next = when (_uiState.value.trackingMode) {
            MapTrackingMode.IDLE -> MapTrackingMode.FOLLOWING
            MapTrackingMode.FOLLOWING -> MapTrackingMode.IDLE
        }
        _uiState.update { it.copy(trackingMode = next) }
        when (next) {
            MapTrackingMode.FOLLOWING -> startLocationPolling()
            MapTrackingMode.IDLE -> stopLocationPolling()
        }
    }

    /**
     * 사용자 제스처(지도 드래그 등)로 트래킹이 해제될 때 호출.
     * KakaoMapView에서 카메라 이동 리스너를 통해 트리거된다.
     */
    fun onTrackingDisabled() {
        if (_uiState.value.trackingMode == MapTrackingMode.IDLE) return
        _uiState.update { it.copy(trackingMode = MapTrackingMode.IDLE) }
        stopLocationPolling()
        Timber.d("사용자 제스처로 트래킹 비활성화")
    }

    /**
     * FOLLOWING 모드에서 현재 위치를 3초마다 폴링하여 UI 상태에 반영한다.
     * currentLocation이 변경되면 KakaoMapView의 LaunchedEffect가 레이블을 이동시킨다.
     */
    private fun startLocationPolling() {
        locationPollingJob?.cancel()
        locationPollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val location = locationManager.getCurrentLocationOrLast()
                if (location != null) {
                    _uiState.update { it.copy(currentLocation = location) }
                    Timber.d("위치 폴링 업데이트: ${location.latitude}, ${location.longitude}")
                }
                delay(3_000L)
            }
        }
    }

    /** 위치 폴링 중단 및 currentLocation 초기화 */
    private fun stopLocationPolling() {
        locationPollingJob?.cancel()
        locationPollingJob = null
        _uiState.update { it.copy(currentLocation = null) }
    }

    // ─── Lottie 아바타 빌드 ─────────────────────────────────────────────────────

    /**
     * 팔로워 목록의 Lottie JSON을 병렬로 생성하여 userId → JSON 맵으로 반환한다.
     *
     * 실패한 항목은 null로 저장되어 UI에서 이니셜 fallback을 표시한다.
     */
    private suspend fun buildFollowerLottieMap(
        activities: List<FollowerRecentActivity>,
    ): Map<Long, String?> = coroutineScope {
        activities
            .map { activity ->
                async(Dispatchers.IO) {
                    val json = runCatching {
                        val character = Character(
                            headImage = CharacterImage(
                                imageName = activity.headImageName,
                                itemTag = activity.headItemTag,
                            ),
                            bodyImage = CharacterImage(
                                imageName = activity.bodyImageName,
                                itemTag = null,
                            ),
                            feetImage = null,
                            grade = Grade.fromApiString(activity.grade),
                        )
                        val baseJson = loadBaseLottieJson(character)
                        lottieImageProcessor.updateCharacterPartsInLottie(baseJson, character).toString()
                    }.getOrElse { t ->
                        Timber.e(t, "팔로워(userId=${activity.userId}) Lottie 생성 실패")
                        null
                    }
                    activity.userId to json
                }
            }
            .awaitAll()
            .toMap()
    }

    /**
     * followerRecords(1km 반경 핀) 각각에 대해 Bitmap을 빌드한다.
     *
     * - 세션 캐시(pinBitmapCache) 히트 시 재빌드 없이 즉시 반환
     * - lottieJsonMap에서 JSON 조회 후 첫 프레임을 Bitmap으로 렌더링
     * - JSON이 없거나 렌더 실패 시 null → KakaoMapView에서 ic_pin_friend 폴백
     */
    private suspend fun buildFollowerPinBitmapMap(
        records: List<FollowerMapRecord>,
        lottieJsonMap: Map<Long, String?>,
    ): Map<Long, Bitmap?> = coroutineScope {
        records.map { record ->
            async(Dispatchers.Default) {
                // 세션 캐시 히트
                if (pinBitmapCache.containsKey(record.userId)) {
                    return@async record.userId to pinBitmapCache[record.userId]
                }
                val json = lottieJsonMap[record.userId]
                val bitmap = json?.let { renderLottieFirstFrame(it, sizePx = 128) }
                pinBitmapCache[record.userId] = bitmap
                record.userId to bitmap
            }
        }.awaitAll().toMap()
    }

    /**
     * Lottie JSON 문자열을 핀 모양 Bitmap으로 렌더링한다.
     *
     * 핀 구조:
     *  - 원형 영역 (40dp): 캐릭터를 증명사진 스타일로 표시 (다리 잘림)
     *  - 정삼각형 꼭지 (높이 8dp): 핀 바텀 포인터
     *
     * Composition 파싱은 Default 디스패처, draw()는 메인 스레드에서 수행한다.
     */
    private suspend fun renderLottieFirstFrame(json: String, sizePx: Int = 0): Bitmap? {
        val density = getApplication<Application>().resources.displayMetrics.density
        val outerCirclePx = (40 * density).roundToInt() // 외부 파란 원 지름 (40dp)
        val innerCirclePx = (26 * density).roundToInt() // 내부 캐릭터 원 지름 (26dp)
        val triHeightPx   = (8  * density).roundToInt()

        val composition = withContext(Dispatchers.Default) {
            runCatching {
                com.airbnb.lottie.LottieCompositionFactory.fromJsonStringSync(json, null).value
            }.getOrElse { t ->
                Timber.e(t, "Lottie composition 파싱 실패")
                null
            }
        } ?: return null

        return withContext(Dispatchers.Main) {
            runCatching {
                val charRenderW = innerCirclePx * 2
                val charRenderH = (innerCirclePx * 2.8f).roundToInt()
                val charBitmap = Bitmap.createBitmap(charRenderW, charRenderH, Bitmap.Config.ARGB_8888)

                com.airbnb.lottie.LottieDrawable().apply {
                    this.composition = composition
                    setFrame(0)
                    // "p" 필드가 "data:image/png;base64,..." 형태로 embed된 경우
                    // Lottie SDK 파일 로더가 처리하지 못하므로 delegate에서 직접 디코딩
                    setImageAssetDelegate { asset ->
                        val fileName = asset.fileName ?: return@setImageAssetDelegate null
                        if (fileName.startsWith("data:")) {
                            runCatching {
                                val bytes = android.util.Base64.decode(
                                    fileName.substringAfter("base64,"),
                                    android.util.Base64.DEFAULT,
                                )
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }.getOrElse { t ->
                                Timber.e(t, "base64 이미지 디코딩 실패: ${asset.id}")
                                null
                            }
                        } else null
                    }
                    setBounds(0, 0, charRenderW, charRenderH)
                    draw(Canvas(charBitmap))
                }

                buildPinBitmap(charBitmap, outerCirclePx, innerCirclePx, triHeightPx)
            }.getOrElse { t ->
                Timber.e(t, "핀 Bitmap 렌더 실패")
                null
            }
        }
    }

    /**
     * 캐릭터 Bitmap을 핀 모양으로 합성한다.
     *
     * 구조:
     *  - 외부 파란 원 (outerCirclePx): 핀 배경
     *  - 내부 흰 원   (innerCirclePx): 캐릭터 표시 영역
     *  - 정삼각형 꼭지 (triHeightPx): 핀 바텀 포인터
     *
     * @param charBitmap      세로로 길게 렌더링된 캐릭터 Bitmap
     * @param outerCirclePx   외부 파란 원 지름 (px)
     * @param innerCirclePx   내부 캐릭터 원 지름 (px)
     * @param triHeightPx     정삼각형 꼭지 높이 (px)
     */
    private fun buildPinBitmap(
        charBitmap: Bitmap,
        outerCirclePx: Int,
        innerCirclePx: Int,
        triHeightPx: Int,
    ): Bitmap {
        // 정삼각형: 밑변 = 높이 × 2/√3
        val triBasePx = (triHeightPx * 2.0 / sqrt(3.0)).roundToInt()
        val totalW = outerCirclePx
        val totalH = outerCirclePx + triHeightPx

        val output = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cx = totalW / 2f
        val cy = outerCirclePx / 2f
        val outerRadius = outerCirclePx / 2f
        val innerRadius = innerCirclePx / 2f

        // 1. 파란 외부 원 + 삼각형
        paint.color = Color.parseColor("#5B93FF")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, outerRadius, paint)
        val triOverlapPx = (2 * getApplication<Application>().resources.displayMetrics.density)
        canvas.drawPath(Path().apply {
            moveTo(cx - triBasePx / 2f, outerCirclePx - triOverlapPx)
            lineTo(cx + triBasePx / 2f, outerCirclePx - triOverlapPx)
            lineTo(cx, totalH.toFloat())
            close()
        }, paint)

        // 2. 흰색 내부 원
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, innerRadius, paint)

        // 3. 캐릭터를 내부 원 클립 안에 그리기
        //    너비를 내부 원에 맞추고 높이는 비율 유지 → 원형 클립이 하단을 자름
        //    charTop을 조정해 머리가 원 안에 잘 들어오도록 위치 설정
        canvas.save()
        canvas.clipPath(Path().apply {
            addCircle(cx, cy, innerRadius - 1f, Path.Direction.CW)
        })
        val scale   = innerCirclePx.toFloat() / charBitmap.width
        val destW   = innerCirclePx.toFloat()
        val destH   = charBitmap.height * scale
        val charLeft = cx - destW / 2f
        // 원 중심보다 약간 아래에서 시작 → 머리가 원 중앙 위쪽에 위치
        val charTop  = cy - destH * 0.38f
        canvas.drawBitmap(
            charBitmap, null,
            RectF(charLeft, charTop, charLeft + destW, charTop + destH),
            paint,
        )
        canvas.restore()
        charBitmap.recycle()

        return output
    }

    /**
     * 캐릭터 등급에 맞는 base Lottie JSON을 raw 리소스에서 로드한다.
     */
    private suspend fun loadBaseLottieJson(character: Character): JSONObject =
        withContext(Dispatchers.IO) {
            val resId = when (character.grade) {
                Grade.SEED -> R.raw.seed
                Grade.SPROUT -> R.raw.sprout
                Grade.TREE -> R.raw.tree
            }
            try {
                val json = getApplication<Application>().resources
                    .openRawResource(resId)
                    .bufferedReader()
                    .use { it.readText() }
                JSONObject(json)
            } catch (t: Throwable) {
                Timber.e(t, "base Lottie JSON 로드 실패: grade=${character.grade}")
                JSONObject()
            }
        }

    companion object {
        private const val SEOUL_CITY_HALL_LAT = 37.5662952
        private const val SEOUL_CITY_HALL_LON = 126.9779692
    }
}
