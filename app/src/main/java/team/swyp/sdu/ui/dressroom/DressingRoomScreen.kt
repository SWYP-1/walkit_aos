package team.swyp.sdu.ui.dressroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.domain.service.LottieImageProcessor
import team.swyp.sdu.ui.components.*
import team.swyp.sdu.ui.dressroom.component.CartDialog
import team.swyp.sdu.ui.dressroom.component.CharacterAndBackground
import team.swyp.sdu.ui.dressroom.component.ItemCard
import team.swyp.sdu.ui.dressroom.component.ItemHeader
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.utils.DateUtils
import team.swyp.sdu.utils.Season

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
    val wornItemsByPosition by viewModel.wornItemsByPosition.collectAsStateWithLifecycle()

    DressingRoomScreen(
        modifier = modifier,
        uiState = uiState,
        cartItems = cartItems,
        lottieImageProcessor = viewModel.lottieImageProcessor, // 실제 주입
        isWearLoading = isWearLoading,
        onBackClick = onNavigateBack,
        onRefreshClick = viewModel::loadDressingRoom,
        onQuestionClick = {},
        onItemClick = { itemId ->
            if (!isWearLoading) { // 로딩 중 클릭 방지
                viewModel.selectItem(itemId)
            }
        },
        onPurChaseItem = { viewModel.purchaseItems() },
        onSaveItem = { viewModel.saveItems() },
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
    wornItemsByPosition: Map<EquipSlot, Int> = emptyMap(),
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQuestionClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onPurChaseItem: () -> Unit,
    onSaveItem: () -> Unit,
) {
    val showCartDialog = remember { mutableStateOf(false) }

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
                is DressingRoomUiState.Success -> SuccessContent(
                    uiState = uiState,
                    cartItems = cartItems,
                    lottieImageProcessor = lottieImageProcessor,
                    wornItemsByPosition = wornItemsByPosition,
                    onBackClick = onBackClick,
                    onRefreshClick = onRefreshClick,
                    onQuestionClick = onQuestionClick,
                    onItemClick = onItemClick,
                    onSaveItem = onSaveItem,
                    onPurChaseItem = { showCartDialog.value = true }
                )
            }

            if (showCartDialog.value) {
                BottomDialog(onDismissRequest = { showCartDialog.value = false }) {
                    CartDialog(
                        cartItems = cartItems.toList(),
                        myPoints = (uiState as? DressingRoomUiState.Success)?.myPoint ?: 0,
                        onDismiss = { showCartDialog.value = false },
                        onPurchase = { itemsToPurchase ->
                            onPurChaseItem()
                            showCartDialog.value = false
                        }
                    )
                }
            }

            // 로딩 오버레이 (착용 요청 중)
            if (isWearLoading) {
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
private fun SuccessContent(
    uiState: DressingRoomUiState.Success,
    cartItems: LinkedHashSet<CosmeticItem>,
    lottieImageProcessor: LottieImageProcessor?,
    wornItemsByPosition: Map<EquipSlot, Int>,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQuestionClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onSaveItem: () -> Unit,
    onPurChaseItem: () -> Unit,
) {
    // 체크박스 상태를 remember로 선언
    var check by remember { mutableStateOf(false) }
    val currentSeason = DateUtils.getCurrentSeason()
    val seasionBackgroundColor = when (currentSeason) {
        Season.SPRING -> SemanticColor.stateGreenPrimary
        Season.SUMMER -> SemanticColor.stateGreenPrimary
        Season.AUTUMN -> SemanticColor.stateGreenPrimary
        Season.WINTER -> SemanticColor.backgroundWhitePrimary
    }

    Box(
        modifier = Modifier
            .background(seasionBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
            // CTA 버튼 높이만큼 패딩
        ) {
            // 캐릭터 영역
            uiState.character?.let { character ->
                CharacterAndBackground(
                    currentSeason = currentSeason,
                    character = character,
                    points = uiState.myPoint,
                    lottieImageProcessor = lottieImageProcessor,
                    onBackClick = onBackClick,
                    onRefreshClick = onRefreshClick,
                    onQuestionClick = onQuestionClick
                )
            }

            // 체크박스 토글 가능한 헤더
            ItemHeader(
                checked = check,
                onCheckedChange = { checked -> check = checked }
            )

            if (uiState.items.isEmpty()) {
                EmptyContent()
            } else {
                ItemGrid(
                    items = uiState.items,
                    selectedItemIds = uiState.selectedItemIdSet,
                    wornItemsByPosition = wornItemsByPosition,
                    onItemClick = onItemClick
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
            Row(
                modifier = Modifier
                    .padding(16.dp), // 내부 여백
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CtaButton(
                    text = "구매하기",
                    textColor = SemanticColor.buttonPrimaryDefault,
                    buttonColor = SemanticColor.backgroundWhitePrimary,
                    enabled = cartItems.isNotEmpty(),
                    onClick = onPurChaseItem,
                    modifier = Modifier.weight(1f)
                )

                CtaButton(
                    text = "저장하기",
                    onClick = onSaveItem,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = null,
                            tint = SemanticColor.iconWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }
    }
}


/**
 * Item Grid
 */
@Composable
private fun ItemGrid(
    items: List<CosmeticItem>,
    selectedItemIds: Set<Int>,
    wornItemsByPosition: Map<EquipSlot, Int>,
    onItemClick: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            val isWorn = wornItemsByPosition[item.position] == item.itemId
            ItemCard(
                itemImageUrl = item.imageName,
                name = item.name,
                point = item.point,
                isMine = item.owned,
                isSelected = selectedItemIds.contains(item.itemId),
                isWorn = isWorn,
                onClick = { onItemClick(item.itemId) },
                modifier = Modifier.fillMaxWidth()
            )
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
private fun ErrorContent(message: String) { /* 동일 */
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
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
            grade = Grade.TREE,
            headImageName = null
        )

        val items = listOf(
            CosmeticItem(
                itemId = 1,
                imageName = "헤어",
                name = "",
                owned = false,
                point = 200,
                position = EquipSlot.BODY
            ),
            CosmeticItem(
                itemId = 2,
                imageName = "상의",
                name = "",
                owned = true,
                point = 2500,
                position = EquipSlot.HEAD
            ),
            CosmeticItem(
                itemId = 3,
                imageName = "헤어",
                name = "",
                owned = false,
                point = 200,
                position = EquipSlot.FEET
            ),

            )

        DressingRoomScreen(
            uiState = DressingRoomUiState.Success(
                items = items,
                selectedItemId = 2,
                selectedItemIdSet = linkedSetOf(1, 2), // 다중 선택 예시
                character = character,
                myPoint = 12500 // API에서 가져온 포인트 값 예시
            ),
            cartItems = linkedSetOf(items[1], items[2]),
            lottieImageProcessor = null, // ⭐ Preview 핵심
            wornItemsByPosition = mapOf(
                // 착용 상태 예시
                EquipSlot.HEAD to 1, // 첫 번째 HEAD 아이템 착용
                EquipSlot.BODY to 2, // 첫 번째 BODY 아이템 착용
            ),
            onBackClick = {},
            onRefreshClick = {},
            onQuestionClick = {},
            onItemClick = {},
            onSaveItem = {},
            onPurChaseItem = {},
        )
    }
}
