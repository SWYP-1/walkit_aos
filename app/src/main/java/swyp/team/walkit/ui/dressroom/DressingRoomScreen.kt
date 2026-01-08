package swyp.team.walkit.ui.dressroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CosmeticItem
import swyp.team.walkit.domain.model.EquipSlot
import swyp.team.walkit.domain.model.WearState
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.ui.components.*
import swyp.team.walkit.ui.dressroom.component.CartDialog
import swyp.team.walkit.ui.dressroom.component.CharacterAndBackground
import swyp.team.walkit.ui.dressroom.component.CharacterGradeInfoDialog
import swyp.team.walkit.ui.dressroom.component.ItemCard
import swyp.team.walkit.ui.dressroom.component.ItemHeader
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.utils.DateUtils
import swyp.team.walkit.utils.Season
import timber.log.Timber

/**
 * Route (ViewModel 연결)
 */
@Composable
fun DressingRoomRoute(
    modifier: Modifier = Modifier,
    viewModel: DressingRoomViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val isWearLoading by viewModel.isWearLoading.collectAsStateWithLifecycle()
    val isRefreshLoading by viewModel.isRefreshLoading.collectAsStateWithLifecycle()
    val wornItemsByPosition by viewModel.wornItemsByPosition.collectAsStateWithLifecycle()

    // UiState에서 선택 상태 가져오기
    val selectedItemIds = LinkedHashSet<Int>()

    // 선택 상태 변경 로깅
    LaunchedEffect(selectedItemIds) {
        Timber.d("🎨 선택 상태 변경 - selectedItemIds: $selectedItemIds")
    }
    val scope = rememberCoroutineScope()

    val showCartDialog by viewModel.showCartDialog.collectAsStateWithLifecycle()

    DressingRoomScreen(
        modifier = modifier,
        uiState = uiState,
        cartItems = cartItems,
        lottieImageProcessor = viewModel.lottieImageProcessor, // 실제 주입
        isWearLoading = isWearLoading,
        isRefreshLoading = isRefreshLoading,
        showCartDialog = showCartDialog,
        selectedItemIds = selectedItemIds,
        wornItemsByPosition = wornItemsByPosition,
        onBackClick = onNavigateBack,
        onRefreshClick = {
            scope.launch {
                viewModel.refreshCharacterInfo()
            }
        },
        onShowCartDialog = viewModel::openCartDialogState,
        onToggleOwnedOnly = viewModel::toggleShowOwnedOnly,
        onItemClick = { itemId ->
            if (!isWearLoading) { // 로딩 중 클릭 방지

            }
            viewModel.selectItem(itemId)
        },
        onPerformPurchase = { viewModel.performPurchase() },
        onSaveItem = { viewModel.saveItems() },
        onDismissCartDialog = viewModel::dismissCartDialog,
    )
}

/**
 * Screen (UI only)
 */
@Composable
fun DressingRoomScreen(
    modifier: Modifier = Modifier,
    uiState: DressingRoomUiState,
    cartItems: LinkedHashSet<CosmeticItem>,
    lottieImageProcessor: LottieImageProcessor?, // ⭐ nullable
    isWearLoading: Boolean = false,
    isRefreshLoading: Boolean = false,
    showCartDialog: Boolean = false,
    selectedItemIds: Set<Int>,
    wornItemsByPosition: Map<EquipSlot, WearState> = emptyMap(),
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleOwnedOnly: () -> Unit,
    onItemClick: (Int) -> Unit,
    onShowCartDialog: () -> Unit,
    onPerformPurchase: () -> Unit,
    onSaveItem: () -> Unit,
    onDismissCartDialog: () -> Unit = {},
) {
    val showGradeInfoDialog = remember { mutableStateOf(false) }

    // serverWornItems는 이제 파라미터로 받음

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is DressingRoomUiState.Loading -> LoadingContent()
                is DressingRoomUiState.Error -> ErrorContent(uiState.message)
                is DressingRoomUiState.Success ->
                    SuccessContent(
                        wornItemsByPosition = wornItemsByPosition,
                        selectedItemIds = selectedItemIds,
                        uiState = uiState,
                        cartItems = cartItems,
                        lottieImageProcessor = lottieImageProcessor,
                        isRefreshLoading = isRefreshLoading,

                        onBackClick = onBackClick,
                        onRefreshClick = onRefreshClick,
                        onQuestionClick = { showGradeInfoDialog.value = true },
                        onToggleOwnedOnly = onToggleOwnedOnly,
                        onItemClick = onItemClick,
                        onSaveItem = onSaveItem,
                        onShowCartDialog = onShowCartDialog,
                        showGradeInfoDialog = showGradeInfoDialog,
                        processedLottieJson = uiState.processedLottieJson,
                        modifier = Modifier, // DressRoom에서는 기본 modifier 사용
                    ).also {
                        Timber.d("📤 CharacterAndBackground 전달 - processedLottieJson: ${uiState.processedLottieJson?.length ?: 0}자")
                    }

            }

            Timber.d("💬 다이얼로그 표시 상태: $showCartDialog, 장바구니 아이템 수: ${cartItems.size}")

            if (showCartDialog) {
                Timber.d("💬 다이얼로그 표시 시작")
                BottomDialog(onDismissRequest = onDismissCartDialog) {
                    CartDialog(
                        cartItems = cartItems.toList(),
                        myPoints = (uiState as? DressingRoomUiState.Success)?.myPoint ?: 0,
                        onDismiss = onDismissCartDialog,
                        onPurchase = { itemsToPurchase ->
                            // 구매 시작 후 다이얼로그는 ViewModel에서 관리
                            onPerformPurchase()
                            // onDismissCartDialog() 제거 - ViewModel에서 구매 완료 시 닫음
                        }
                    )
                }
            }

            // 로딩 오버레이 (착용 요청 중 또는 새로고침 중)
            if (isWearLoading || isRefreshLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SemanticColor.backgroundWhitePrimary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomProgressIndicator()
                }
            }
        }
    }
}

/**
 * Success UI
 */
@Composable
fun SuccessContent(
    wornItemsByPosition: Map<EquipSlot, WearState>,
    selectedItemIds: Set<Int>,
    uiState: DressingRoomUiState.Success,
    cartItems: LinkedHashSet<CosmeticItem>,
    lottieImageProcessor: LottieImageProcessor?,
    isRefreshLoading: Boolean = false,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQuestionClick: () -> Unit,
    onToggleOwnedOnly: () -> Unit,
    onItemClick: (Int) -> Unit,
    onSaveItem: () -> Unit,
    onShowCartDialog: () -> Unit,
    showGradeInfoDialog: MutableState<Boolean>,
    processedLottieJson: String? = null,
    modifier: Modifier = Modifier,
    // 카테고리 필터 (CharacterShop에서만 사용)
    selectedCategory: EquipSlot? = null,
    onCategoryFilterChange: ((EquipSlot?) -> Unit)? = null,
    showCategoryFilter: Boolean = false,
) {
    // 체크박스 상태는 ViewModel에서 관리됨
    val currentSeason = DateUtils.getCurrentSeason()
    val seasionBackgroundColor = when (currentSeason) {
        Season.SPRING -> SemanticColor.stateGreenPrimary
        Season.SUMMER -> SemanticColor.stateGreenPrimary
        Season.AUTUMN -> SemanticColor.stateGreenPrimary
        Season.WINTER -> SemanticColor.backgroundWhitePrimary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(seasionBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
            // CTA 버튼 높이만큼 패딩
        ) {
            // 캐릭터 영역
            if (uiState.character != null) {
                CharacterAndBackground(
                    currentSeason = currentSeason,
                    character = uiState.character,
                    points = uiState.myPoint,

                    cosmeticItems = uiState.items,
                    lottieImageProcessor = lottieImageProcessor,
                    onBackClick = onBackClick,
                    onRefreshClick = onRefreshClick,
                    onQuestionClick = onQuestionClick,
                    processedLottieJson = processedLottieJson
                )
            } else {
                // 캐릭터 정보 로딩 중 (DB에 없으면 API 호출 중)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(25f / 32f) // CharacterAndBackground의 배경 이미지 aspectRatio와 동일
                        .background(seasionBackgroundColor),
                    contentAlignment = Alignment.Center
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

            // 체크박스 토글 가능한 헤더
            ItemHeader(
                checked = uiState.showOwnedOnly,
                onCheckedChange = { onToggleOwnedOnly() },
                selectedCategory = selectedCategory,
                onCategoryFilterChange = onCategoryFilterChange,
                showCategoryFilter = showCategoryFilter
            )

            if (uiState.items.isEmpty()) {
                EmptyContent()
            } else {
                ItemGrid(
                    items = uiState.items,
                    selectedItemIds = selectedItemIds,
                    wornItemsByPosition = wornItemsByPosition,
                    onItemClick = onItemClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // CTA 버튼 고정
        Surface(
            shadowElevation = 4.dp, // 그림자 높이
            color = SemanticColor.backgroundWhitePrimary, // 배경색
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            CtaButton(
                text = "저장하기",
                onClick = onSaveItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                iconResId = R.drawable.ic_arrow_forward
            )
        }

        // 캐릭터 등급 정보 다이얼로그
        if (showGradeInfoDialog.value) {
            CharacterGradeInfoDialog(
                onDismiss = { showGradeInfoDialog.value = false }
            )
        }

    }
}


/**
 * Item Grid
 */
@Composable
private fun ItemGrid(
    modifier : Modifier,
    items: List<CosmeticItem>,
    selectedItemIds: Set<Int>,
    wornItemsByPosition: Map<EquipSlot, WearState>,
    onItemClick: (Int) -> Unit,
) {
    // LazyVerticalGrid 대신 Row + chunked() 방식으로 3열 그리드 구현
    val rows = items.chunked(3) // 3열씩 그룹화

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedItemIds.contains(item.itemId)

                    ItemCard(
                        itemImageUrl = item.imageName,
                        position = item.position, // EquipSlot 직접 전달
                        name = item.position.displayName,
                        point = item.point,
                        isMine = item.owned,
                        isSelected = isSelected,
                        onClick = { onItemClick(item.itemId) },
                        modifier = Modifier.weight(1f) // Row 내에서 균등 분배
                    )
                }
                // 빈 칸 채우기 (3열 유지) - 아이템 개수가 3의 배수가 아닐 때
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Empty / Error / Loading
 */
@Composable
private fun EmptyContent() { /* 동일 */
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "아이템 목록을 불러오는데 실패했습니다",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
    }
}

/**
 * Preview
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewDressingRoomFullSample() {
    WalkItTheme {
        val character = Character(
            nickName = "캐릭터 기본",
            grade = swyp.team.walkit.domain.model.Grade.SEED,
            headImage = null
        )

        val items = listOf(
            CosmeticItem(
                itemId = 1,
                imageName = "헤어",
                name = "",
                owned = false,
                worn = false,
                point = 200,
                position = EquipSlot.BODY
            ),
            CosmeticItem(
                itemId = 2,
                imageName = "상의",
                name = "",
                owned = true,
                worn = true,  // 미리보기용으로 worn=true 설정
                point = 2500,
                position = EquipSlot.HEAD
            ),
            CosmeticItem(
                itemId = 3,
                imageName = "헤어",
                name = "",
                owned = false,
                worn = false,
                point = 200,
                position = EquipSlot.FEET
            ),

            )

        DressingRoomScreen(
            uiState = DressingRoomUiState.Success(
                items = items,
                character = character,
                myPoint = 12500, // API에서 가져온 포인트 값 예시
                showOwnedOnly = false
            ),
            cartItems = linkedSetOf(items[1]),
            lottieImageProcessor = null, // ⭐ Preview 핵심
            showCartDialog = false,
            selectedItemIds = LinkedHashSet(setOf(1, 2)), // 선택된 아이템 ID들
            wornItemsByPosition = mapOf(
                // 착용 상태 예시
                EquipSlot.HEAD to WearState.Worn(2), // HEAD 아이템 착용
                EquipSlot.BODY to WearState.Default, // BODY 기본 상태
                EquipSlot.FEET to WearState.Unworn  // FEET 미착용 상태
            ),
            onBackClick = {},
            onRefreshClick = {},
            onToggleOwnedOnly = {},
            onItemClick = {},
            onSaveItem = {},
            onDismissCartDialog = {},
            onShowCartDialog = {},
            onPerformPurchase = {}
        )
    }
}
