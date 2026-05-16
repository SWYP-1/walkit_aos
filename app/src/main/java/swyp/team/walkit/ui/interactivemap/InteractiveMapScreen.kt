package swyp.team.walkit.ui.interactivemap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import swyp.team.walkit.data.model.MapMarker
import swyp.team.walkit.data.model.MapPin
import swyp.team.walkit.presentation.viewmodel.KakaoMapViewModel
import swyp.team.walkit.ui.components.KakaoMapView
import swyp.team.walkit.ui.interactivemap.bottomtab.BottomSheetDragHandle
import swyp.team.walkit.ui.interactivemap.bottomtab.SpotBottomSheetContent
import swyp.team.walkit.ui.interactivemap.components.ExpandableAvatarRow
import swyp.team.walkit.ui.interactivemap.components.MapSearchBar
import swyp.team.walkit.ui.interactivemap.components.MapTrackingButton
import kotlin.math.roundToInt

/** 바텀시트 3단계 스냅 앵커 */
enum class SheetAnchor { MIN, MID, MAX }

private val SHEET_MIN_HEIGHT = 82.dp
private val SHEET_MID_HEIGHT = 350.dp
private const val SHEET_MAX_FRACTION = 0.9f

@Composable
fun InteractiveMapRoute(
    modifier: Modifier = Modifier,
    onNavigateToFriends: () -> Unit = {},
    onNavigateToFriendDetail: (userId: Long, walkId: Long) -> Unit = { _, _ -> },
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
    interactiveViewModel: InteractiveMapViewModel = hiltViewModel(),
) {
    val uiState by interactiveViewModel.uiState.collectAsStateWithLifecycle()
    val mapPins by interactiveViewModel.mapPins.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * SHEET_MAX_FRACTION

    val sheetState = remember { AnchoredDraggableState(initialValue = SheetAnchor.MIN) }

    // 화면 높이 확정 후 3단계 앵커 등록
    // offset 0 = 최대(위), 커질수록 아래로 내려감
    LaunchedEffect(maxHeight) {
        with(density) {
            sheetState.updateAnchors(
                DraggableAnchors {
                    SheetAnchor.MAX at 0f
                    SheetAnchor.MID at (maxHeight - SHEET_MID_HEIGHT).toPx()
                    SheetAnchor.MIN at (maxHeight - SHEET_MIN_HEIGHT).toPx()
                }
            )
        }
    }

    // 화면 최초 진입 시 현재 위치 기반으로 지도 데이터 전체 로드
    LaunchedEffect(Unit) {
        interactiveViewModel.loadMapDataFromCurrentLocation()
    }

    // 스팟 시트 확장/축소 이벤트 → 3단계 앵커로 매핑
    LaunchedEffect(Unit) {
        interactiveViewModel.spotSheetEvents.collect { event ->
            when (event) {
                SpotSheetEvent.ExpandSheet -> scope.launch {
                    sheetState.animateTo(SheetAnchor.MAX)
                }
                SpotSheetEvent.SnapToExpand -> scope.launch {
                    // 한 프레임 대기 → 콘텐츠가 리컴포즈된 뒤 expand 시작
                    withFrameNanos { }
                    sheetState.animateTo(SheetAnchor.MAX)
                }
                SpotSheetEvent.PartialExpand -> scope.launch {
                    sheetState.animateTo(SheetAnchor.MIN)
                }
            }
        }
    }

    // 친구 상세 화면 이동 이벤트
    LaunchedEffect(Unit) {
        interactiveViewModel.friendDetailNavEvent.collect { event ->
            onNavigateToFriendDetail(event.userId, event.walkId)
        }
    }

    // 검색/상세 상태에서 시스템 뒤로가기 처리
    val isInSubState = uiState.spotSheetContent != SpotSheetContent.SpotList
    BackHandler(enabled = isInSubState) {
        when (uiState.spotSheetContent) {
            is SpotSheetContent.SpotDetail -> interactiveViewModel.onSpotDetailClose()
            SpotSheetContent.Search -> interactiveViewModel.onSpotSearchBack()
            SpotSheetContent.RecentSearch -> interactiveViewModel.onRecentSearchBack()
            SpotSheetContent.SpotList -> Unit
        }
    }

    InteractiveMapScreen(
        modifier = modifier,
        uiState = uiState,
        mapPins = mapPins,
        sheetState = sheetState,
        sheetMinHeight = SHEET_MIN_HEIGHT,
        sheetMaxHeight = maxHeight,
        mapViewModel = mapViewModel,
        onMarkerClick = interactiveViewModel::onMarkerClick,
        onZoomChanged = interactiveViewModel::onZoomChanged,
        onAvatarClick = interactiveViewModel::onAvatarClick,
        onNavigateToFriends = onNavigateToFriends,
        onSpotSearchIconClick = interactiveViewModel::onSpotSearchIconClick,
        onSpotQueryChange = interactiveViewModel::onSpotSearchQueryChange,
        onSpotSearch = interactiveViewModel::onSpotSearch,
        onSpotSearchClear = interactiveViewModel::onSpotSearchClear,
        onSpotItemClick = interactiveViewModel::onSpotGridItemClick,
        onSpotDetailClose = interactiveViewModel::onSpotDetailClose,
        onSpotSearchBack = interactiveViewModel::onSpotSearchBack,
        onRecentSearchBack = interactiveViewModel::onRecentSearchBack,
        onRecentSearchClick = interactiveViewModel::onRecentSearchClick,
        onRemoveRecentSearch = interactiveViewModel::onRemoveRecentSearch,
        onClearAllRecentSearch = interactiveViewModel::onClearAllRecentSearches,
        onRefreshMapSearch = interactiveViewModel::loadMapDataFromCurrentLocation,
        onTrackingClick = interactiveViewModel::onTrackingButtonClick,
        onTrackingDisabled = interactiveViewModel::onTrackingDisabled,
    )
}

@Composable
fun InteractiveMapScreen(
    modifier: Modifier = Modifier,
    uiState: InteractiveMapUiState,
    mapPins: List<MapPin> = emptyList(),
    sheetState: AnchoredDraggableState<SheetAnchor>,
    sheetMinHeight: Dp,
    sheetMaxHeight: Dp,
    mapViewModel: KakaoMapViewModel,
    onMarkerClick: (MapMarker) -> Unit,
    onZoomChanged: (Int) -> Unit = {},
    onAvatarClick: (userId: Long) -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onSpotSearchIconClick: () -> Unit = {},
    onSpotQueryChange: (String) -> Unit = {},
    onSpotSearch: () -> Unit = {},
    onSpotSearchClear: () -> Unit = {},
    onSpotItemClick: (swyp.team.walkit.domain.model.NearbySpot) -> Unit = {},
    onSpotDetailClose: () -> Unit = {},
    onSpotSearchBack: () -> Unit = {},
    onRecentSearchBack: () -> Unit = {},
    onRecentSearchClick: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearAllRecentSearch: () -> Unit = {},
    onRefreshMapSearch: () -> Unit = {},
    onTrackingClick: () -> Unit = {},
    onTrackingDisabled: () -> Unit = {},
) {
    val density = LocalDensity.current

    // 드래그 핸들에 적용할 flingBehavior (스냅 스프링 애니메이션 포함)
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = sheetState,
        positionalThreshold = { distance -> distance * 0.5f },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
    )

    // 현재 시트 offset (NaN이면 MIN 위치로 폴백)
    val offsetPx by remember {
        derivedStateOf {
            sheetState.offset.takeUnless { it.isNaN() }
                ?: with(density) { (sheetMaxHeight - sheetMinHeight).toPx() }
        }
    }

    // 시트가 최소 높이보다 얼마나 더 올라갔는지 → 지도 위 버튼 위치 계산용
    val extraRiseDp by remember {
        derivedStateOf {
            with(density) {
                val minOffsetPx = (sheetMaxHeight - sheetMinHeight).toPx()
                (minOffsetPx - offsetPx).coerceAtLeast(0f).toDp()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 지도 (전체 화면)
        KakaoMapView(
            locations = emptyList(),
            viewModel = mapViewModel,
            modifier = Modifier.fillMaxSize(),
            markers = uiState.allMarkers,
            pins = mapPins,
            friendBitmaps = uiState.followerPinBitmapMap,
            onMarkerClick = onMarkerClick,
            onZoomChanged = onZoomChanged,
            trackingMode = uiState.trackingMode,
            currentLocation = uiState.currentLocation,
            centerLocation = uiState.mapCenterLocation,
            onTrackingDisabled = onTrackingDisabled,
        )

        // 팔로우 아바타 행 (지도 상단 오버레이)
        ExpandableAvatarRow(
            modifier = Modifier.align(Alignment.TopStart),
            activities = uiState.recentActivities,
            lottieJsonMap = uiState.followerLottieJsonMap,
            onNavigateToFriends = onNavigateToFriends,
            onAvatarClick = onAvatarClick,
        )

        // 위치 추적 버튼 — 시트 상단 16dp 위 고정, 시트가 올라올수록 같이 올라감
        MapTrackingButton(
            trackingMode = uiState.trackingMode,
            onClick = onTrackingClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = sheetMinHeight + 16.dp + extraRiseDp),
        )

        // 현 지도에서 검색 버튼 — 시트 상단 16dp 위 고정, 시트가 올라올수록 같이 올라감
        MapSearchBar(
            onClick = onRefreshMapSearch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = sheetMinHeight + 16.dp + extraRiseDp),
        )

        // 에러 메시지
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            )
        }

        // 3단계 스냅 바텀시트
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetMaxHeight)
                .offset { IntOffset(0, offsetPx.roundToInt()) }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    clip = false,
                )
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White),
        ) {
            Column(Modifier.fillMaxSize()) {
                // 드래그 핸들 영역 (이 부분만 드래그 가능 → 아래 LazyList 스크롤과 충돌 방지)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .anchoredDraggable(
                            state = sheetState,
                            orientation = Orientation.Vertical,
                            flingBehavior = flingBehavior,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomSheetDragHandle()
                }

                // 스팟 콘텐츠 (스크롤 가능, 드래그와 충돌 없음)
                SpotBottomSheetContent(
                    content = uiState.spotSheetContent,
                    spots = uiState.spots,
                    searchQuery = uiState.spotSearchQuery,
                    searchResults = uiState.spotSearchResults,
                    isSearching = uiState.isSearchingSpots,
                    recentSearchQueries = uiState.recentSearchQueries,
                    onSearchIconClick = onSpotSearchIconClick,
                    onQueryChange = onSpotQueryChange,
                    onSearch = onSpotSearch,
                    onSearchClear = onSpotSearchClear,
                    onSpotItemClick = onSpotItemClick,
                    onSpotDetailClose = onSpotDetailClose,
                    onSearchBack = onSpotSearchBack,
                    onRecentSearchBack = onRecentSearchBack,
                    onRecentSearchClick = onRecentSearchClick,
                    onRemoveRecentSearch = onRemoveRecentSearch,
                    onClearAllRecentSearch = onClearAllRecentSearch,
                )
            }
        }
    }
}
