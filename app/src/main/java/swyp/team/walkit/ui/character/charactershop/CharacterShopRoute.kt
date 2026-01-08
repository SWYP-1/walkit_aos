package swyp.team.walkit.ui.character.charactershop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import swyp.team.walkit.ui.character.CharacterScreen
import swyp.team.walkit.ui.character.CharacterTabViewModel
import swyp.team.walkit.ui.components.BottomDialog
import swyp.team.walkit.ui.dressroom.DressingRoomUiState
import swyp.team.walkit.ui.dressroom.SuccessContent
import swyp.team.walkit.ui.dressroom.component.CartDialog
import timber.log.Timber

/**
 * 캐릭터 상점 Shop 탭 Route
 * SuccessContent를 사용하되, 내부 그리드를 Row + chunked() 방식으로 구현
 */
@Composable
fun CharacterShopRoute(
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

    val onDismissCartDialog = viewModel::dismissCartDialog
    val onPerformPurchase = viewModel::performPurchase

    val currentUiState = uiState
    Box(
        modifier = Modifier.fillMaxWidth()
            .wrapContentHeight() // LazyColumn item에 맞는 높이 설정
    ) {

        if (currentUiState is DressingRoomUiState.Success) {

            // CharacterShop에서도 Lottie 캐릭터 표시 + 아이템 선택 가능하도록 SuccessContent 사용
            // LazyColumn의 item 안에서 사용되므로 fillMaxSize 대신 wrapContent 사용
            SuccessContent(
                wornItemsByPosition = wornItemsByPosition,
                selectedItemIds = selectedItemIds,
                uiState = currentUiState,
                cartItems = cartItems,
                lottieImageProcessor = viewModel.lottieImageProcessor,
                isRefreshLoading = isRefreshLoading,
                onBackClick = {}, // CharacterShop 탭에서는 뒤로가기 불필요
                onRefreshClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.refreshCharacterInfo()
                    }
                },
                onQuestionClick = {}, // CharacterShop에서는 사용하지 않음
                onToggleOwnedOnly = viewModel::toggleShowOwnedOnly,
                onItemClick = { itemId ->
                    if (!isWearLoading) {
                        viewModel.selectItem(itemId)
                    }
                },
                onSaveItem = { viewModel.saveItems() },
                onShowCartDialog = viewModel::openCartDialogState,
                showGradeInfoDialog = remember {
                    mutableStateOf(
                        false
                    )
                },
                processedLottieJson = currentUiState.processedLottieJson,
                modifier = Modifier, // LazyColumn item에서는 기본 modifier 사용
                selectedCategory = selectedCategory,
                onCategoryFilterChange = viewModel::changeCategoryFilter,
                showCategoryFilter = true
            )
        }

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
    }
}

/**
 * 캐릭터 상점 Route
 */
@Composable
fun CharacterRoute(
) {
    val tabViewModel: CharacterTabViewModel = hiltViewModel()
    val tabUiState by tabViewModel.uiState.collectAsStateWithLifecycle()

    // 메인 탭의 content로 직접 표시 (헤더 없음)
    CharacterScreen(
        tabUiState = tabUiState,
        onTabSelected = tabViewModel::onTabSelected,
        modifier = Modifier.fillMaxSize()
    )
}
