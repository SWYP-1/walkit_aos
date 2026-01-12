package swyp.team.walkit.ui.charactershop

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import swyp.team.walkit.ui.character.component.CharacterCategorySection

import swyp.team.walkit.ui.components.BottomDialog
import swyp.team.walkit.ui.dressroom.DressingRoomUiState
import swyp.team.walkit.ui.dressroom.SuccessContent
import swyp.team.walkit.ui.dressroom.component.CartDialog
import swyp.team.walkit.ui.record.components.customShadow
import swyp.team.walkit.ui.theme.walkItTypography
import timber.log.Timber

/**
 * 캐릭터 상점 탭 UI 상태
 */
data class CharacterShopTabUiState(
    val selectedTabIndex: Int = 0,
)

/**
 * 캐릭터 상점 탭 ViewModel (탭 선택만 담당)
 */
@HiltViewModel
class CharacterShopTabViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterShopTabUiState())
    val uiState: StateFlow<CharacterShopTabUiState> = _uiState.asStateFlow()

    /**
     * 탭 선택
     */
    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }
}

/**
 * 캐릭터 상점 Shop 탭 Route
 */
@Composable
fun CharacterShopShopRoute(
) {
    val viewModel: CharacterShopViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIdSet.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val isWearLoading by viewModel.isWearLoading.collectAsStateWithLifecycle()
    val isRefreshLoading by viewModel.isRefreshLoading.collectAsStateWithLifecycle()

    val wornItemsByPosition by viewModel.wornItemsByPosition.collectAsStateWithLifecycle()
    val showCartDialog by viewModel.showCartDialog.collectAsStateWithLifecycle()

    val onDismissCartDialog = viewModel::dismissCartDialog
    val onPerformPurchase = viewModel::performPurchase

    val currentUiState = uiState
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (currentUiState is DressingRoomUiState.Success) {

            // CharacterShop에서도 Lottie 캐릭터 표시 + 아이템 선택 가능하도록 SuccessContent 사용
            // 높이 제약을 위해 Column으로 감싸서 제한된 높이 제공
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                SuccessContent(
                    wornItemsByPosition = wornItemsByPosition,
                    selectedItemIds = selectedItemIds,
                    uiState = currentUiState,
                    cartItems = cartItems,
                    lottieImageProcessor = viewModel.lottieImageProcessor,
                    isRefreshLoading = isRefreshLoading,
                    onBackClick = {}, // CharacterShop 탭에서는 뒤로가기 불필요
                    onRefreshClick = {
                        viewModel.loadDressingRoom()
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
                    showGradeInfoDialog = androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(
                            false
                        )
                    },
                    processedLottieJson = currentUiState.processedLottieJson,
                    modifier = Modifier.weight(1f) // 중요: 제한된 높이 제공
                )
            }
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
fun CharacterShopRoute(
) {
    val tabViewModel: CharacterShopTabViewModel = hiltViewModel()
    val tabUiState by tabViewModel.uiState.collectAsStateWithLifecycle()

    // 메인 탭의 content로 직접 표시 (헤더 없음)
    CharacterShopScreen(
        tabUiState = tabUiState,
        onTabSelected = tabViewModel::onTabSelected,
        modifier = Modifier.fillMaxSize()
    )
}
/**
 * 캐릭터 상점 Screen
 */
@Composable
fun CharacterShopScreen(
    tabUiState: CharacterShopTabUiState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 탭 레이아웃 (RecordTabRow와 동일한 디자인)
        CharacterShopTabRow(
            selectedTabIndex = tabUiState.selectedTabIndex,
            onTabSelected = onTabSelected,
        )

        // 탭 콘텐츠
        CharacterShopTabContent(
            selectedTab = CharacterShopTabType.entries[tabUiState.selectedTabIndex],
        )
    }
}

/**
 * 캐릭터 상점 탭 타입
 */
enum class CharacterShopTabType {
    Category, // 캐릭터 카테고리
    Shop,     // 아이템 상점
}

/**
 * 캐릭터 상점 탭 행 컴포넌트 (RecordTabRow와 동일한 디자인)
 */
@Composable
fun CharacterShopTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = CharacterShopTabType.values()
    val containerShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val tabShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .customShadow()
            .border(
                width = 1.dp,
                color = swyp.team.walkit.ui.theme.SemanticColor.textBorderSecondaryInverse,
                shape = containerShape
            )
            .background(
                color = swyp.team.walkit.ui.theme.SemanticColor.backgroundWhitePrimary,
                shape = containerShape
            )
            .padding(vertical = 8.dp, horizontal = 7.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTabIndex == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(tabShape)
                        .background(
                            color = if (selected)
                                swyp.team.walkit.ui.theme.SemanticColor.stateAquaBluePrimary
                            else
                                androidx.compose.ui.graphics.Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = when (tab) {
                            CharacterShopTabType.Category -> "카테고리"
                            CharacterShopTabType.Shop -> "아이템 상점"
                        },
                        color = if (selected)
                            swyp.team.walkit.ui.theme.SemanticColor.textBorderPrimaryInverse
                        else
                            swyp.team.walkit.ui.theme.SemanticColor.textBorderSecondary,
                        style = androidx.compose.material3.MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

/**
 * 캐릭터 상점 탭 콘텐츠
 */
@Composable
fun CharacterShopTabContent(
    selectedTab: CharacterShopTabType,
) {
    when (selectedTab) {
        CharacterShopTabType.Category -> {
            // 캐릭터 카테고리 섹션 컴포넌트 사용
            CharacterCategorySection()
        }

        CharacterShopTabType.Shop -> {
            // CharacterShop Shop 탭 Route 사용
            CharacterShopShopRoute()
        }
    }
}
