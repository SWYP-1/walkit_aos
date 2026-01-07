package swyp.team.walkit.ui.charactershop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 캐릭터 상점 Route
 */
@Composable
fun CharacterShopRoute(
) {
    val viewModel: CharacterShopViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 메인 탭의 content로 직접 표시 (헤더 없음)
    CharacterShopScreen(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        modifier = Modifier.fillMaxSize()
    )
}
/**
 * 캐릭터 상점 Screen
 */
@Composable
fun CharacterShopScreen(
    uiState: CharacterShopUiState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 탭 레이아웃
        CharacterShopTabLayout(
            selectedTabIndex = uiState.selectedTabIndex,
            onTabSelected = onTabSelected,
        )

        // 탭 콘텐츠
        CharacterShopTabContent(
            selectedTab = CharacterShopTabType.entries[uiState.selectedTabIndex],
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
 * 캐릭터 상점 탭 레이아웃
 */
@Composable
fun CharacterShopTabLayout(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    // TODO: 탭 레이아웃 구현
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
            // 아이템 상점 섹션 컴포넌트 사용
        }
    }
}

/**
 * 캐릭터 카테고리 섹션
 */
@Composable
fun CharacterCategorySection() {
    // TODO: 캐릭터 카테고리 구현
}

/**
 * 아이템 상점 섹션
 */
@Composable
fun ItemShopSection() {
    // TODO: 아이템 상점 구현
}
