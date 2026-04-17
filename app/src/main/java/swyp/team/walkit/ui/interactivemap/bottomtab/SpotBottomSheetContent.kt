package swyp.team.walkit.ui.interactivemap.bottomtab

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.NearbySpot
import swyp.team.walkit.ui.components.EmptyResultScreen
import swyp.team.walkit.ui.components.SearchBar
import swyp.team.walkit.ui.interactivemap.SpotSheetContent
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 스팟 바텀시트 루트 콘텐츠
 *
 * SpotList / Search / SpotDetail 세 가지 상태를 AnimatedContent로 전환한다.
 *
 * @param content                현재 시트 내용 상태
 * @param spots                  주변 추천 스팟 목록 (SpotList 상태에서 표시)
 * @param searchQuery            검색어
 * @param searchResults          검색 결과 목록
 * @param isSearching            검색 로딩 중 여부
 * @param recentSearchQueries    최근 검색어 목록 (RecentSearch 상태에서 표시)
 * @param onSearchIconClick      돋보기 아이콘 클릭 콜백
 * @param onQueryChange          검색어 변경 콜백
 * @param onSearch               검색 실행 콜백
 * @param onSearchClear          검색어 초기화 콜백
 * @param onSpotItemClick        스팟 아이템 클릭 콜백 (SpotDetail로 이동)
 * @param onSpotDetailClose      SpotDetail X 버튼 콜백 (SpotList로 복귀)
 * @param onSearchBack           검색 뒤로가기 콜백 (RecentSearch로 복귀)
 * @param onRecentSearchBack     최근검색 뒤로가기 콜백 (SpotList로 복귀)
 * @param onRecentSearchClick    최근 검색어 항목 클릭 콜백
 * @param onRemoveRecentSearch   최근 검색어 개별 삭제 콜백
 * @param onClearAllRecentSearch 최근 검색어 전체 삭제 콜백
 */
@Composable
fun SpotBottomSheetContent(
    content: SpotSheetContent,
    spots: List<NearbySpot>,
    searchQuery: String,
    searchResults: List<NearbySpot>,
    isSearching: Boolean,
    recentSearchQueries: List<String>,
    onSearchIconClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchClear: () -> Unit,
    onSpotItemClick: (NearbySpot) -> Unit,
    onSpotDetailClose: () -> Unit,
    onSearchBack: () -> Unit,
    onRecentSearchBack: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearAllRecentSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = content,
        // SpotDetail은 spot별로 다른 composable로 취급 → 이전 이미지 ghost 방지
        contentKey = { state ->
            when (state) {
                is SpotSheetContent.SpotDetail -> state.spot.thumbnailUrl
                else -> state::class
            }
        },
        transitionSpec = {
            val enter = when (targetState) {
                SpotSheetContent.RecentSearch,
                SpotSheetContent.Search -> slideInHorizontally { it } + fadeIn()
                else -> fadeIn()
            }
            // SpotList에서 나갈 때는 즉시 사라짐 → 전환 중 SpotList 노출 방지
            val exit = when (initialState) {
                SpotSheetContent.RecentSearch,
                SpotSheetContent.Search -> slideOutHorizontally { it } + fadeOut()
                SpotSheetContent.SpotList -> ExitTransition.None
                else -> fadeOut()
            }
            enter togetherWith exit
        },
        label = "spot_sheet_content",
        modifier = modifier.fillMaxWidth(),
    ) { target ->
        when (target) {
            SpotSheetContent.SpotList -> SpotListContent(
                spots = spots,
                onSearchIconClick = onSearchIconClick,
                onSpotItemClick = onSpotItemClick,
            )

            SpotSheetContent.RecentSearch -> RecentSearchContent(
                searchQuery = searchQuery,
                recentQueries = recentSearchQueries,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSearchClear = onSearchClear,
                onRecentItemClick = onRecentSearchClick,
                onRemoveItem = onRemoveRecentSearch,
                onClearAll = onClearAllRecentSearch,
                onBack = onRecentSearchBack,
            )

            SpotSheetContent.Search -> SpotSearchContent(
                searchQuery = searchQuery,
                searchResults = searchResults,
                isSearching = isSearching,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSearchClear = onSearchClear,
                onSpotItemClick = onSpotItemClick,
                onBack = onSearchBack,
            )

            is SpotSheetContent.SpotDetail -> RecommendSpotPinBottomTab(
                spot = target.spot,
                onClose = onSpotDetailClose,
            )
        }
    }
}

// ─── SpotList ────────────────────────────────────────────────────────────────

/**
 * 기본 상태: "주변 추천 스팟" 헤더 + 2열 그리드
 */
@Composable
private fun SpotListContent(
    spots: List<NearbySpot>,
    onSearchIconClick: () -> Unit,
    onSpotItemClick: (NearbySpot) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 헤더 — 항상 72dp 내에서 노출되어 peek 상태에서도 보임
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "주변 추천 스팟",
                style = MaterialTheme.walkItTypography.bodyXL.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColor.textBorderPrimary,
                ),
            )
            Box(
                modifier = Modifier
                    .size(33.dp)
                    .clip(CircleShape)
                    .background(SemanticColor.backgroundWhiteTertiary)
                    .clickable(onClick = onSearchIconClick),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_search),
                    contentDescription = "장소 검색",
                    tint = SemanticColor.iconGrey,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))


        // 스팟 그리드

        if (spots.isEmpty()) {
            EmptyResultScreen(
                title = "주변 추천 스팟이 없어요",
                subtitle = "다른 위치에서 다시 검색해보세요",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            SpotGrid(spots = spots, onItemClick = onSpotItemClick)
        }
        Spacer(Modifier.height(20.5.dp))
    }
}

// ─── Search ──────────────────────────────────────────────────────────────────

/**
 * 검색 상태: 뒤로가기 + 검색바 + 결과 2열 그리드
 */
@Composable
private fun SpotSearchContent(
    searchQuery: String,
    searchResults: List<NearbySpot>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchClear: () -> Unit,
    onSpotItemClick: (NearbySpot) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 검색바 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_backward),
                    contentDescription = "뒤로가기",
                    tint = SemanticColor.iconBlack,
                    modifier = Modifier.size(24.dp),
                )
            }
            SearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onClear = onSearchClear,
                onSearch = onSearch,
                placeholder = "장소를 검색해보세요.",
                modifier = Modifier.weight(1f),
            )
        }

        // 검색 결과
        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "검색 결과가 없습니다.",
                        style = MaterialTheme.walkItTypography.bodyM,
                        color = SemanticColor.textBorderTertiary,
                    )
                }
            }

            searchResults.isNotEmpty() -> {
                SpotGrid(spots = searchResults, onItemClick = onSpotItemClick)
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}

// ─── 공통 그리드 ──────────────────────────────────────────────────────────────

/**
 * 스팟 2열 그리드
 */
@Composable
private fun SpotGrid(
    spots: List<NearbySpot>,
    onItemClick: (NearbySpot) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
    ) {
        items(spots) { spot ->
            SpotGridItem(spot = spot, onClick = { onItemClick(spot) })
        }
    }
}

/**
 * 스팟 그리드 아이템 — 썸네일 + 장소명 + 거리
 */
@Composable
private fun SpotGridItem(
    spot: NearbySpot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(spot.thumbnailUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_default_user)
                .error(R.drawable.ic_default_user)
                .build(),
            contentDescription = spot.placeName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = spot.placeName,
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColor.textBorderPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${spot.distance}m",
                style = MaterialTheme.walkItTypography.captionM,
                color = SemanticColor.textBorderTertiary,
            )
        }
    }
}

// ─── RecentSearch ─────────────────────────────────────────────────────────────

/**
 * 최근 검색어 상태: 검색바 + "최근 검색" 헤더 + 목록
 */
@Composable
private fun RecentSearchContent(
    searchQuery: String,
    recentQueries: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchClear: () -> Unit,
    onRecentItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 검색바 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_backward),
                    contentDescription = "뒤로가기",
                    tint = SemanticColor.iconGrey,
                    modifier = Modifier.size(24.dp),
                )
            }
            SearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onClear = onSearchClear,
                onSearch = onSearch,
                placeholder = "장소를 검색해보세요.",
                backgroundColor = SemanticColor.backgroundWhitePrimary,
                borderColor = SemanticColor.textBorderSecondaryInverse,
                modifier = Modifier.weight(1f),
            )
        }

        // 헤더: 최근 검색 + 지우기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "최근 검색",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColor.textBorderSecondary,
                ),
            )
            if (recentQueries.isNotEmpty()) {
                Text(
                    text = "지우기",
                    style = MaterialTheme.walkItTypography.captionM,
                    color = SemanticColor.textBorderTertiary,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClearAll,
                    ),
                )
            }
        }

        if (recentQueries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "최근 검색어가 없습니다.",
                    style = MaterialTheme.walkItTypography.bodyM,
                    color = SemanticColor.textBorderTertiary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
            ) {
                itemsIndexed(
                    items = recentQueries,
                    key = { index, item -> item } // 그대로 써도 되고 index 섞어도 됨
                ) { index, query ->

                    RecentSearchItem(
                        query = query,
                        onClick = { onRecentItemClick(query) },
                        onRemove = { onRemoveItem(query) },
                    )
                    // 마지막 아이템이면 Divider 안 그림
                    if (index < recentQueries.lastIndex) {
                        HorizontalDivider(
                            Modifier
                                .height(2.dp)
                                .padding(vertical = 4.dp),
                            color = SemanticColor.backgroundWhiteSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}

/**
 * 최근 검색어 단일 행 — 텍스트 + X 버튼
 */
@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = query,
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Medium,
                color = SemanticColor.textBorderPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_clear),
                contentDescription = "삭제",
                tint = SemanticColor.iconGrey,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
