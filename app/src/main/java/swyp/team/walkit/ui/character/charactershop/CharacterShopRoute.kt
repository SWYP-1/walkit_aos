package swyp.team.walkit.ui.character.charactershop

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import swyp.team.walkit.ui.components.BottomDialog
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.dressroom.DressingRoomUiState
import swyp.team.walkit.ui.dressroom.component.CartDialog
import swyp.team.walkit.ui.dressroom.component.CharacterAndBackground
import swyp.team.walkit.ui.dressroom.component.CharacterGradeInfoDialog
import swyp.team.walkit.ui.dressroom.component.ItemCard
import swyp.team.walkit.ui.components.InfoBanner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor

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
    val infoBannerMessage by viewModel.infoBannerMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // 화면 진입 시마다 포인트 갱신 (보상 받기 등으로 포인트가 변경되었을 수 있음)
    LaunchedEffect(Unit) {
        viewModel.refreshPoint()
    }
    val showGradeInfoDialog = remember { mutableStateOf(false) }

    // InfoBanner 표시 상태 관리
    var showInfoBanner by remember { mutableStateOf(false) }

    // 아이템 클릭 핸들러
    val onItemClick: (Int) -> Unit = { itemId ->
        if (!isWearLoading) {
            viewModel.selectItem(itemId)
        }
    }

    // InfoBanner 메시지 표시
    LaunchedEffect(infoBannerMessage) {
        infoBannerMessage?.let {
            showInfoBanner = true
            // 3초 후 자동으로 사라짐
            delay(3000)
            showInfoBanner = false
        }
    }
    val onRefreshClick = viewModel::refreshCharacterInfo
    // 화면 높이 계산
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val peekHeight = screenHeight * 0.4f
    val maxSheetHeight = screenHeight * 0.8f
    val scope = rememberCoroutineScope()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true
        )
    )

    if (uiState is DressingRoomUiState.Success) {
        val successState = uiState as DressingRoomUiState.Success

        // MainScreen의 padding을 무시하고 상태바 영역만 직접 처리
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .fillMaxSize()
                .background(SemanticColor.backgroundWhiteSecondary)

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
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                        12.dp
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = 90.dp
                                    )
                                ) {
                                    // 필터 헤더
                                    item(span = {
                                        androidx.compose.foundation.lazy.grid.GridItemSpan(
                                            3
                                        )
                                    }) {
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
                modifier = Modifier.fillMaxSize(),
                sheetPeekHeight = peekHeight,
                sheetDragHandle = null
            ) {
                // scaffold content: 캐릭터 표시 (CTA 버튼 제외)
                CharacterShopScaffoldContent(
                    uiState = successState,
                    lottieImageProcessor = viewModel.lottieImageProcessor,
                    isRefreshLoading = isRefreshLoading,
                    isWearLoading = isWearLoading,
                    onRefreshClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.refreshCharacterInfo()
                        }
                    },
                    onClickQuestion = {
                        showGradeInfoDialog.value = true
                    },
                    processedLottieJson = successState.processedLottieJson,
                    showCtaButton = false // CTA 버튼 숨기기
                )
            }

            // InfoBanner (바텀 네비게이션바 위 16dp 떨어진 곳에 표시)
            // MainScreen의 Scaffold가 이미 시스템 바를 처리하므로 바텀 네비게이션바 높이만 고려
            if (showInfoBanner && infoBannerMessage != null) {
                val bottomNavigationBarHeight = 56.dp // 바텀 네비게이션바 높이 (아이콘 24dp + 텍스트 + 패딩)
                val infoBannerBottomPadding = bottomNavigationBarHeight + 16.dp

                Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(bottom = infoBannerBottomPadding)
                        .zIndex(3f) // 가장 위에 표시
                        .padding(horizontal = 16.dp)
                ) {
                    InfoBanner(
                        title = infoBannerMessage?.title ?: "",
                        description = infoBannerMessage?.description,
                        backgroundColor = SemanticColor.backgroundDarkSecondary,
                        borderColor = SemanticColor.backgroundDarkSecondary,
                        iconTint = SemanticColor.iconWhite,
                        textColor = SemanticColor.textBorderPrimaryInverse,
                        modifier = Modifier.fillMaxWidth(),
                        icon = { iconTint ->
                            Icon(
                                painter = painterResource(R.drawable.ic_info_check),
                                contentDescription = "info warning",
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        })
                }
            }

            // CTA 버튼 (가장 위에 배치, 바텀 시트 위로 표시)
            Surface(
                shadowElevation = 4.dp,
                color = swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .zIndex(1f) // 가장 위에 표시
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = SemanticColor.textBorderGreenPrimary,
                                shape = RoundedCornerShape(size = 8.dp)
                            )
                            .width(48.dp)
                            .height(46.dp)
                            .background(
                                color = SemanticColor.backgroundWhitePrimary,
                                shape = RoundedCornerShape(size = 8.dp)
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            )
                            .clickable(onClick = {
                                scope.launch {
                                    viewModel.refreshCharacterInfo()
                                }
                            })
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_refresh),
                            contentDescription = "refresh",
                            tint = SemanticColor.stateGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    CtaButton(
                        text = "저장하기",
                        onClick = { viewModel.saveItems() },
                        enabled = !isWearLoading,
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }

            }
        }

        if (showCartDialog) {
            BottomDialog(onDismissRequest = viewModel::dismissCartDialog) {
                CartDialog(
                    cartItems = cartItems.toList(),
                    myPoints = (uiState as? DressingRoomUiState.Success)?.myPoint ?: 0,
                    onDismiss = viewModel::dismissCartDialog,
                    onPurchase = { viewModel.performPurchase() })
            }
        }
        // 캐릭터 등급 정보 다이얼로그
        if (showGradeInfoDialog.value) {
            CharacterGradeInfoDialog(
                onDismiss = { showGradeInfoDialog.value = false })
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
    onRefreshClick: () -> Unit,
    onClickQuestion: () -> Unit,
    processedLottieJson: String?,
    showCtaButton: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 캐릭터 표시 영역 (스크롤 가능)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
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
                    onQuestionClick = onClickQuestion, // CharacterShop에서는 사용하지 않음
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