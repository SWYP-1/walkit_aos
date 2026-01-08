package swyp.team.walkit.ui.character.charactershop

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import swyp.team.walkit.ui.character.CharacterScreen
import swyp.team.walkit.ui.components.BottomDialog
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.dressroom.DressingRoomUiState
import swyp.team.walkit.ui.dressroom.component.CartDialog
import swyp.team.walkit.ui.dressroom.component.CharacterAndBackground
import swyp.team.walkit.ui.dressroom.component.ItemCard

/**
 * 캐릭터 상점 Shop 탭 Route
 * BottomSheetScaffold를 사용하여 캐릭터 표시 영역과 아이템 그리드 영역을 분리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterShopRoute(
    modifier: Modifier = Modifier,
) {
    val viewModel: CharacterShopViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIdSet.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val isWearLoading by viewModel.isWearLoading.collectAsStateWithLifecycle()
    val isRefreshLoading by viewModel.isRefreshLoading.collectAsStateWithLifecycle()
    val wornItemsByPosition by viewModel.wornItemsByPosition.collectAsStateWithLifecycle()
    val showCartDialog by viewModel.showCartDialog.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // 아이템 클릭 핸들러
    val onItemClick: (Int) -> Unit = { itemId ->
        if (!isWearLoading) {
            viewModel.selectItem(itemId)
        }
    }

    toastMessage?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
    }

    // 화면 높이 계산
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val peekHeight = screenHeight * 0.4f
    val maxSheetHeight = screenHeight * 0.8f

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    if (uiState is DressingRoomUiState.Success) {
        val successState = uiState as DressingRoomUiState.Success

        androidx.compose.foundation.layout.Box(
            modifier = modifier.fillMaxSize()
        ) {
            // 바텀 시트 (캐릭터 표시 + 그리드)
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    // 바텀 시트 콘텐츠 (최대 높이 제한)
                    Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .background(swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary)
                            .heightIn(max = maxSheetHeight)
                    ) {
                        // 아이템 그리드 영역 (남은 공간 차지)
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val gridState = rememberLazyGridState()

                            if (successState.items.isEmpty()) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "아이템이 없습니다",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    state = gridState,
                                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = 90.dp  // 🎛️ CTA 버튼 가림 방지용 패딩 증가
                                    )
                                ) {
                                    // 필터 헤더
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                        swyp.team.walkit.ui.dressroom.component.ItemHeader(
                                            checked = successState.showOwnedOnly,
                                            onCheckedChange = { viewModel.toggleShowOwnedOnly() },
                                            selectedCategory = selectedCategory,
                                            onCategoryFilterChange = viewModel::changeCategoryFilter,
                                            showCategoryFilter = true
                                        )
                                    }

                                    // 아이템들
                                    items(successState.items.size) { index ->
                                        val item = successState.items[index]
                                        val isSelected = selectedItemIds.contains(item.itemId)

                                        ItemCard(
                                            itemImageUrl = item.imageName,
                                            position = item.position,
                                            name = item.position.displayName,
                                            point = item.point,
                                            isMine = item.owned,
                                            isSelected = isSelected,
                                            onClick = { onItemClick(item.itemId) },
                                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                sheetPeekHeight = peekHeight,
                sheetDragHandle = null
            ) {
                // scaffold content: 캐릭터 표시 (CTA 버튼 제외)
                CharacterShopScaffoldContent(
                    uiState = successState,
                    lottieImageProcessor = viewModel.lottieImageProcessor,
                    isRefreshLoading = isRefreshLoading,
                    isWearLoading = isWearLoading,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected,
                    onRefreshClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.refreshCharacterInfo()
                        }
                    },
                    processedLottieJson = successState.processedLottieJson,
                    showCtaButton = false // CTA 버튼 숨기기
                )
            }

            // CTA 버튼 (가장 위에 배치, 바텀 시트 위로 표시)
            androidx.compose.material3.Surface(
                shadowElevation = 4.dp,
                color = swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .zIndex(1f) // 가장 위에 표시
            ) {
                swyp.team.walkit.ui.components.CtaButton(
                    text = "저장하기",
                    onClick = { viewModel.saveItems() },
                    enabled = !isWearLoading,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    iconResId = swyp.team.walkit.R.drawable.ic_arrow_forward
                )
            }
        }

        if (showCartDialog) {
            BottomDialog(onDismissRequest = viewModel::dismissCartDialog) {
                CartDialog(
                    cartItems = cartItems.toList(),
                    myPoints = (uiState as? DressingRoomUiState.Success)?.myPoint ?: 0,
                    onDismiss = viewModel::dismissCartDialog,
                    onPurchase = { viewModel.performPurchase() }
                )
            }
        }
    }
}


/**
 * BottomSheet Scaffold Content (캐릭터 표시 영역 + 탭 row)
 */
@Composable
fun CharacterShopScaffoldContent(
    uiState: DressingRoomUiState.Success,
    lottieImageProcessor: swyp.team.walkit.domain.service.LottieImageProcessor?,
    isRefreshLoading: Boolean,
    isWearLoading: Boolean,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onRefreshClick: () -> Unit,
    processedLottieJson: String?,
    showCtaButton: Boolean = true
) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        // 캐릭터 표시 영역 (스크롤 가능)
       Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp) // 캐릭터 영역의 고정 높이
        ) {
            if (uiState.character != null) {
                CharacterAndBackground(
                    currentSeason = swyp.team.walkit.utils.DateUtils.getCurrentSeason(),
                    character = uiState.character,
                    points = uiState.myPoint,
                    cosmeticItems = uiState.items,
                    lottieImageProcessor = lottieImageProcessor,
                    onBackClick = {}, // CharacterShop에서는 뒤로가기 불필요
                    onRefreshClick = onRefreshClick,
                    onQuestionClick = {}, // CharacterShop에서는 사용하지 않음
                    processedLottieJson = processedLottieJson
                )
            } else {
                // 캐릭터 정보 로딩 중
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "캐릭터 정보를 불러오는 중...",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        )
                    )
                }
            }
        }

        // CTA 버튼 (항상 맨 아래 고정) - showCtaButton이 true일 때만 표시
        if (showCtaButton) {
            androidx.compose.material3.Surface(
                shadowElevation = 4.dp,
                color = swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                CtaButton(
                    text = "저장하기",
                    onClick = { },
                    enabled = !isWearLoading,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    iconResId = swyp.team.walkit.R.drawable.ic_arrow_forward
                )
            }
        }
    }
}


// CharacterShopSheetContent 함수는 sheetContent에서 직접 구현으로 대체되었습니다.

/*
// BottomSheet Sheet Content (아이템 그리드 및 필터 영역)
@Composable
fun CharacterShopSheetContent(
    uiState: DressingRoomUiState.Success,
    selectedItemIds: Set<Int>,
    wornItemsByPosition: Map<EquipSlot, WearState>,
    selectedCategory: EquipSlot?,
    isWearLoading: Boolean,
    onToggleOwnedOnly: () -> Unit,
    onItemClick: (Int) -> Unit,
    onCategoryFilterChange: (EquipSlot?) -> Unit,
    onSaveItem: () -> Unit,
    onShowCartDialog: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .background(swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary)
    ) {
        // BottomSheet drag handle (항상 보이는 고정 영역)
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        color = swyp.team.walkit.ui.theme.SemanticColor.textBorderTertiary,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }

        // 스크롤 가능한 콘텐츠 영역
        val gridState = rememberLazyGridState()

        if (uiState.items.isEmpty()) {
            // Empty 상태
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "아이템이 없습니다",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.6f
                    )
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                    12.dp
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp // CTA 버튼이 그리드 안에 있으므로 기본 패딩만
                )
            ) {
                // 필터 헤더를 그리드의 첫 번째 아이템으로
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    swyp.team.walkit.ui.dressroom.component.ItemHeader(
                        checked = uiState.showOwnedOnly,
                        onCheckedChange = { onToggleOwnedOnly() },
                        selectedCategory = selectedCategory,
                        onCategoryFilterChange = onCategoryFilterChange,
                        showCategoryFilter = true
                    )
                }

                // 아이템들 (스크롤 가능)
                items(uiState.items.size) { index ->
                    val item = uiState.items[index]
                    val isSelected = selectedItemIds.contains(item.itemId)

                    ItemCard(
                        itemImageUrl = item.imageName,
                        position = item.position,
                        name = item.position.displayName,
                        point = item.point,
                        isMine = item.owned,
                        isSelected = isSelected,
                        onClick = { onItemClick(item.itemId) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // CTA 버튼 (항상 맨 아래 고정)
        androidx.compose.material3.Surface(
            shadowElevation = 4.dp,
            color = swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            swyp.team.walkit.ui.components.CtaButton(
                text = "저장하기",
                onClick = onSaveItem,
                enabled = !isWearLoading,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                iconResId = swyp.team.walkit.R.drawable.ic_arrow_forward
            )
        }
    }
}

*/