package team.swyp.sdu.ui.dressroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.ui.components.BottomDialog
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.dressroom.component.CartDialog
import team.swyp.sdu.ui.dressroom.component.CharacterAndBackground
import team.swyp.sdu.ui.dressroom.component.ItemCard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 드레싱룸 화면
 *
 * 코스메틱 아이템을 보고 선택할 수 있는 화면입니다.
 */


@Composable
fun DressingRoomRoute(
    modifier: Modifier = Modifier,
    viewModel: DressingRoomViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()

    DressingRoomScreen(
        modifier = modifier,
        cartItems = cartItems,
        uiState = uiState,
        onBackClick = onNavigateBack,
        onRefreshClick = { /* 필요시 ViewModel 연동 */ },
        onQuestionClick = { /* 필요시 질문 클릭 처리 */ },
        onItemClick = { productId ->
            viewModel.selectItem(productId)
        },
        onPurChaseItem = {},
        onSaveItem = {},
    )
}

@Composable
fun DressingRoomScreen(
    modifier: Modifier = Modifier,
    uiState: DressingRoomUiState,
    cartItems: LinkedHashSet<CosmeticItem>,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQuestionClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onPurChaseItem : () -> Unit,
    onSaveItem : () -> Unit,
) {
    // 다이얼로그 표시 상태
    val showCartDialog = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {
        when (uiState) {
            is DressingRoomUiState.Loading -> LoadingContent()
            is DressingRoomUiState.Error -> ErrorContent(errorMessage = uiState.message)
            is DressingRoomUiState.Success -> SuccessContent(
                cartItems = cartItems,
                uiState = uiState,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onQuestionClick = onQuestionClick,
                onItemClick = onItemClick,
                onSaveItem = onSaveItem,
                onPurChaseItem = {
                    // 구매 버튼 클릭 시 다이얼로그 열기
                    showCartDialog.value = true
                },
            )
        }

        // 2️⃣ CartDialog 바텀 다이얼로그
        if (showCartDialog.value) {
            BottomDialog(onDismissRequest = { showCartDialog.value = false }) {
                CartDialog(
                    cartItems = cartItems.toList(),
                    myPoints = 5700,
                    onDismiss = { showCartDialog.value = false },
                    onPurchase = { itemsToPurchase ->
                        // 실제 구매 처리
                        onPurChaseItem()
                        showCartDialog.value = false
                    }
                )
            }
        }
    }
}


@Composable
private fun SuccessContent(
    uiState: DressingRoomUiState.Success,
    cartItems : LinkedHashSet<CosmeticItem>,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQuestionClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onSaveItem : () -> Unit,
    onPurChaseItem: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // 헤더 + 배경
        uiState.character?.let { character ->
            CharacterAndBackground(
                character = character,
                points = uiState.myPoint,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onQuestionClick = onQuestionClick,
            )
        }

        // 아이템 그리드
        if (uiState.items.isEmpty()) {
            EmptyContent()
        } else {
            Column {



                ItemGrid(
                    items = uiState.items,
                    selectedItemId = uiState.selectedItemId,
                    onItemClick = onItemClick
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CtaButton(
                        text = "구매하기",
                        textColor = SemanticColor.buttonPrimaryDefault,
                        buttonColor = SemanticColor.backgroundWhitePrimary,
                        onClick = onPurChaseItem,
                        enabled = !cartItems.isEmpty(),
                        modifier = Modifier.width(96.dp)      // 1
                    )

                    CtaButton(
                        text = "저장하기",
                        textColor = SemanticColor.textBorderPrimaryInverse,
                        onClick = onSaveItem,
                        modifier = Modifier.weight(1f),   // 2.4
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_forward),
                                contentDescription = "arrow forward",
                                tint = SemanticColor.iconWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }

        }
    }
}


/**
 * 아이템 그리드
 */
@Composable
private fun ItemGrid(
    items: List<team.swyp.sdu.domain.model.CosmeticItem>,
    selectedItemId: Int?,
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
            ItemCard(
                itemImageUrl = item.imageName,
                name = item.name,
                price = 120,
                isMine = item.owned,
                isSelected = selectedItemId == item.itemId,
                onClick = { onItemClick(item.itemId) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 빈 콘텐츠 UI
 */
@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "아이템이 없습니다",
                style = MaterialTheme.walkItTypography.bodyL,
                color = SemanticColor.textBorderSecondary
            )
            Text(
                text = "다른 카테고리를 선택해보세요",
                style = MaterialTheme.walkItTypography.bodyM,
                color = SemanticColor.textBorderTertiary
            )
        }
    }
}

/**
 * 에러 상태 UI
 */
@Composable
private fun ErrorContent(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "오류가 발생했습니다", style = MaterialTheme.walkItTypography.bodyL.copy(
                    fontWeight = FontWeight.SemiBold
                ), color = SemanticColor.stateRedPrimary
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.walkItTypography.bodyM,
                color = SemanticColor.textBorderSecondary
            )
        }
    }
}


/**
 * 로딩 상태 UI
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CustomProgressIndicator(
            size = ProgressIndicatorSize.Medium, color = SemanticColor.stateBluePrimary
        )
    }
}

// 샘플 데이터
private val sampleItems = linkedSetOf(
    CosmeticItem(itemId = 1, name = "헤어스타일1", imageName = "", owned = false, price = 200, position = EquipSlot.BODY),
    CosmeticItem(itemId = 2, name = "상의1", imageName = "", owned = true, price = 300,position = EquipSlot.BODY),
    CosmeticItem(itemId = 3, name = "하의1", imageName = "", owned = false, price = 250,position = EquipSlot.BODY)
)


@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewDressingRoomFullSample() {
    WalkItTheme {
        // 다이얼로그 상태
        val showCartDialog = remember { mutableStateOf(true) }

        // 샘플 캐릭터
        val sampleCharacter = Character(
            nickName = "캐릭터 기본",
            grade = Grade.TREE
        )

        // 샘플 아이템 리스트
        val sampleItemList = listOf(
            CosmeticItem(itemId = 1, name = "헤어스타일1", imageName = "R.drawable.ic_hair_1.toString()", owned = false, price = 200, position = EquipSlot.BODY),
            CosmeticItem(itemId = 2, name = "상의1", imageName = "R.drawable.ic_top_1.toString()", owned = true, price = 300, position = EquipSlot.BODY),
            CosmeticItem(itemId = 3, name = "하의1", imageName = "R.drawable.ic_bottom_1.toString()", owned = false, price = 250, position = EquipSlot.BODY),
            CosmeticItem(itemId = 4, name = "모자1", imageName =" R.drawable.ic_hat_1.toString()", owned = true, price = 150, position = EquipSlot.HEAD)
        )

        Column {
            DressingRoomScreen(
                uiState = DressingRoomUiState.Success(
                    items = sampleItemList,
                    selectedItemId = 2,
                    character = sampleCharacter,
                    myPoint = 5700
                ),
                cartItems = linkedSetOf(sampleItemList[1], sampleItemList[3]),
                onBackClick = {},
                onRefreshClick = {},
                onQuestionClick = {},
                onItemClick = {},
                onSaveItem = {},
                onPurChaseItem = { showCartDialog.value = true }
            )

            if (showCartDialog.value) {
                BottomDialog(onDismissRequest = { showCartDialog.value = false }) {
                    CartDialog(
                        cartItems = sampleItemList.subList(0, 2),
                        myPoints = 5700,
                        onDismiss = { showCartDialog.value = false },
                        onPurchase = {}
                    )
                }
            }
        }
    }
}
