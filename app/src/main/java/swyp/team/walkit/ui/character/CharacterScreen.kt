package swyp.team.walkit.ui.character

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import swyp.team.walkit.ui.character.charactershop.CharacterShopRoute
import swyp.team.walkit.ui.character.component.CharacterCategorySection


/**
 * 캐릭터 상점 탭 콘텐츠
 * LazyColumn의 item으로 사용되는 각 탭의 콘텐츠
 */
@Composable
private fun CharacterShopTabContent(
    selectedTab: CharacterTabType,
) {
    when (selectedTab) {
        CharacterTabType.Category -> {
            // 캐릭터 카테고리 섹션 컴포넌트 사용
            CharacterCategorySection()
        }

        CharacterTabType.Shop -> {
            // CharacterShop Shop 탭 Route 사용
            CharacterShopRoute()
        }
    }
}

/**
 * 캐릭터 상점 Screen - LazyColumn 기반 구조
 *
 * 구조:
 * LazyColumn
 * ├── item: CharacterShopTabRow (탭 선택)
 * └── item: CharacterShopTabContent (선택된 탭의 콘텐츠)
 */
@Composable
fun CharacterScreen(
    tabUiState: CharacterTabUiState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        // 탭 레이아웃 item
        item {
            CharacterShopTabRow(
                selectedTabIndex = tabUiState.selectedTabIndex,
                onTabSelected = onTabSelected,
            )
        }

        // 탭 콘텐츠 item
        item {
            CharacterShopTabContent(
                selectedTab = CharacterTabType.entries[tabUiState.selectedTabIndex],
            )
        }
    }
}