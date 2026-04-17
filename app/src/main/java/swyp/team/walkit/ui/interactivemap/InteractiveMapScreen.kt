package swyp.team.walkit.ui.interactivemap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import swyp.team.walkit.data.model.MapMarker
import swyp.team.walkit.presentation.viewmodel.KakaoMapViewModel
import swyp.team.walkit.ui.components.KakaoMapView
import swyp.team.walkit.ui.interactivemap.bottomtab.BottomSheetDragHandle
import swyp.team.walkit.ui.interactivemap.bottomtab.SpotBottomSheetContent
import swyp.team.walkit.ui.interactivemap.components.ExpandableAvatarRow
import swyp.team.walkit.ui.interactivemap.components.MapSearchBar
import swyp.team.walkit.ui.interactivemap.components.MapTrackingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMapRoute(
    modifier: Modifier = Modifier,
    onNavigateToFriends: () -> Unit = {},
    onNavigateToFriendDetail: (userId: Long, walkId: Long) -> Unit = { _, _ -> },
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
    interactiveViewModel: InteractiveMapViewModel = hiltViewModel(),
) {
    val uiState by interactiveViewModel.uiState.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    // 화면 최초 진입 시 현재 위치 기반으로 지도 데이터 전체 로드
    LaunchedEffect(Unit) {
        interactiveViewModel.loadMapDataFromCurrentLocation()
    }

    // 스팟 시트 확장/축소 이벤트
    LaunchedEffect(Unit) {
        interactiveViewModel.spotSheetEvents.collect { event ->
            when (event) {
                SpotSheetEvent.ExpandSheet -> scope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
                SpotSheetEvent.SnapToExpand -> scope.launch {
                    // 한 프레임 대기 → 콘텐츠(RecentSearch)가 리컴포즈된 뒤 expand 시작
                    // 이렇게 하면 SpotList가 렌더링되기 전에 콘텐츠가 이미 교체된 상태로 시트가 열림
                    withFrameNanos { }
                    scaffoldState.bottomSheetState.expand()
                }
                SpotSheetEvent.PartialExpand -> scope.launch {
                    scaffoldState.bottomSheetState.partialExpand()
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
        scaffoldState = scaffoldState,
        mapViewModel = mapViewModel,
        onMarkerClick = interactiveViewModel::onMarkerClick,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMapScreen(
    modifier: Modifier = Modifier,
    uiState: InteractiveMapUiState,
    scaffoldState: BottomSheetScaffoldState,
    mapViewModel: KakaoMapViewModel,
    onMarkerClick: (MapMarker) -> Unit,
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
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = Color.White,
        topBar = {
//            AppHeader(
//                title = "지도",
//                showBackButton = false,
//            )
        },
        sheetDragHandle = { BottomSheetDragHandle() },
        sheetPeekHeight = 82.dp,
        sheetContent = {
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
        },
        modifier = modifier,
    ) { innerPadding ->
        val density = LocalDensity.current

        // peek 상태일 때의 offset을 기준값으로 한 번만 기록
        // → 절대 좌표 대신 delta만 사용하므로 좌표계 차이 무관
        var peekOffsetPx by remember { mutableStateOf<Float?>(null) }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            peekOffsetPx = runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrNull()
        }

        // 시트가 peek보다 얼마나 올라갔는지 (px)
        val extraRisePx by remember {
            derivedStateOf {
                val peek = peekOffsetPx ?: return@derivedStateOf 0f
                val current = runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrDefault(peek)
                (peek - current).coerceAtLeast(0f)
            }
        }
        val extraRiseDp = with(density) { extraRisePx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 지도
            KakaoMapView(
                locations = emptyList(),
                viewModel = mapViewModel,
                modifier = Modifier.fillMaxSize(),
                markers = uiState.allMarkers,
                friendBitmaps = uiState.followerPinBitmapMap,
                onMarkerClick = onMarkerClick,
                trackingMode = uiState.trackingMode,
                currentLocation = uiState.currentLocation,
                onTrackingDisabled = onTrackingDisabled,
            )

            // 팔로우 아바타 행 (지도 상단 오버레이)
            ExpandableAvatarRow(
                modifier = Modifier.align(Alignment.TopStart),
                activities = uiState.recentActivities,
                lottieJsonMap = uiState.followerLottieJsonMap,
                onNavigateToFriends = onNavigateToFriends,
            )

            // 나침반(위치 추적) 버튼 — 화면 좌측, 시트 상단 16dp 위 고정
            MapTrackingButton(
                trackingMode = uiState.trackingMode,
                onClick = onTrackingClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp + extraRiseDp),
            )
            // 현 지도에서 검색 버튼 — 화면 중앙, 시트 상단 16dp 위 고정
            MapSearchBar(
                onClick = onRefreshMapSearch,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp + extraRiseDp),
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
        }
    }

}
