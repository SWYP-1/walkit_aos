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

// ─── 공개 진입점 ──────────────────────────────────────────────────────────────

/**
 * 드래그 핸들 영역에 포함될 헤더.
 * anchoredDraggable Box 안에 배치하여 터치 편의성을 높인다.
 *
 * - SpotList: "주변 추천 스팟" 제목 + 검색 아이콘
 * - Search / RecentSearch: 뒤로가기 + 검색바 (+ 최근 검색 서브헤더)
 * - SpotDetail: 빈 영역 (별도 헤더 없음)
 */
@Composable
fun SpotSheetHeader(
    content: SpotSheetContent,
    searchQuery: String,
    recentSearchQueries: List<String>,
    onSearchIconClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchClear: () -> Unit,
    onSearchBack: () -> Unit,
    onRecentSearchBack: () -> Unit,
    onClearAllRecentSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = content,
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
            val exit = when (initialState) {
                SpotSheetContent.RecentSearch,
                SpotSheetContent.Search -> slideOutHorizontally { it } + fadeOut()
                SpotSheetContent.SpotList -> ExitTransition.None
                else -> fadeOut()
            }
            enter togetherWith exit
        },
        label = "spot_sheet_header",
        modifier = modifier.fillMaxWidth(),
    ) { target ->
        when (target) {
            SpotSheetContent.SpotList -> SpotListHeader(
                onSearchIconClick = onSearchIconClick,
            )
            SpotSheetContent.Search -> SearchHeader(
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSearchClear = onSearchClear,
                onBack = onSearchBack,
            )
            SpotSheetContent.RecentSearch -> RecentSearchHeader(
                searchQuery = searchQuery,
                recentQueries = recentSearchQueries,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSearchClear = onSearchClear,
                onBack = onRecentSearchBack,
                onClearAll = onClearAllRecentSearch,
            )
            is SpotSheetContent.SpotDetail -> Unit
        }
    }
}

/**
 * 스크롤 가능한 본문 영역 (헤더 제외).
 * anchoredDraggable 영역 밖에 배치한다.
 */
@Composable
fun SpotSheetBody(
    content: SpotSheetContent,
    spots: List<NearbySpot>,
    searchQuery: String,
    searchResults: List<NearbySpot>,
    isSearching: Boolean,
    recentSearchQueries: List<String>,
    onSpotItemClick: (NearbySpot) -> Unit,
    onSpotDetailClose: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = content,
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
            val exit = when (initialState) {
                SpotSheetContent.RecentSearch,
                SpotSheetContent.Search -> slideOutHorizontally { it } + fadeOut()
                SpotSheetContent.SpotList -> ExitTransition.None
                else -> fadeOut()
            }
            enter togetherWith exit
        },
        label = "spot_sheet_body",
        modifier = modifier.fillMaxWidth(),
    ) { target ->
        when (target) {
            SpotSheetContent.SpotList -> SpotListBody(
                spots = spots,
                onSpotItemClick = onSpotItemClick,
            )
            SpotSheetContent.Search -> SearchBody(
                searchQuery = searchQuery,
                searchResults = searchResults,
                isSearching = isSearching,
                onSpotItemClick = onSpotItemClick,
            )
            SpotSheetContent.RecentSearch -> RecentSearchBody(
                recentQueries = recentSearchQueries,
                onRecentItemClick = onRecentSearchClick,
                onRemoveItem = onRemoveRecentSearch,
            )
            is SpotSheetContent.SpotDetail -> RecommendSpotPinBottomTab(
                spot = target.spot,
                onClose = onSpotDetailClose,
            )
        }
    }
}

// ─── SpotList ────────────────────────────────────────────────────────────────

@Composable
private fun SpotListHeader(
    onSearchIconClick: () -> Unit,
) {
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
}

@Composable
private fun SpotListBody(
    spots: List<NearbySpot>,
    onSpotItemClick: (NearbySpot) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(20.dp))
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

@Composable
private fun SearchHeader(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchClear: () -> Unit,
    onBack: () -> Unit,
) {
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
}

@Composable
private fun SearchBody(
    searchQuery: String,
    searchResults: List<NearbySpot>,
    isSearching: Boolean,
    onSpotItemClick: (NearbySpot) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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

// ─── RecentSearch ─────────────────────────────────────────────────────────────

@Composable
private fun RecentSearchHeader(
    searchQuery: String,
    recentQueries: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchClear: () -> Unit,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun RecentSearchBody(
    recentQueries: List<String>,
    onRecentItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                    key = { _, item -> item },
                ) { index, query ->
                    RecentSearchItem(
                        query = query,
                        onClick = { onRecentItemClick(query) },
                        onRemove = { onRemoveItem(query) },
                    )
                    if (index < recentQueries.lastIndex) {
                        HorizontalDivider(
                            Modifier
                                .height(2.dp)
                                .padding(vertical = 4.dp),
                            color = SemanticColor.backgroundWhiteSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}

// ─── 공통 그리드 ──────────────────────────────────────────────────────────────

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
    ) {
        items(spots) { spot ->
            SpotGridItem(spot = spot, onClick = { onItemClick(spot) })
        }
    }
}

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
//            .background(Color(0xFFF8F8F8))
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
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
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
                text = formatDistance(spot.distance),
                style = MaterialTheme.walkItTypography.captionM,
                color = SemanticColor.textBorderTertiary,
            )
        }
        Spacer(Modifier.height(3.dp))
    }
}

// ─── 거리 포맷 ────────────────────────────────────────────────────────────────

/** 미터 단위 문자열을 사람이 읽기 좋은 거리 표현으로 변환. 100m 미만은 m, 이상은 km */
private fun formatDistance(distanceStr: String): String {
    val meters = distanceStr.toDoubleOrNull() ?: return distanceStr
    return if (meters < 100.0) {
        "${meters.toInt()}m"
    } else {
        val tenths = (meters / 100.0).toInt()
        if (tenths % 10 == 0) "${tenths / 10}km" else "${tenths / 10}.${tenths % 10}km"
    }
}

// ─── 최근 검색 아이템 ─────────────────────────────────────────────────────────

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
