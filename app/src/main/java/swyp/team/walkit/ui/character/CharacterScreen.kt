package swyp.team.walkit.ui.character

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import swyp.team.walkit.ui.character.charactershop.CharacterShopRoute
import swyp.team.walkit.ui.character.component.CharacterCategorySection



/**
 * 캐릭터 상점 Screen
 *
 * Shop 탭에서는 BottomSheetScaffold를 사용하므로 LazyColumn을 사용하지 않음
 */
@Composable
fun CharacterScreen(
    tabUiState: CharacterTabUiState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (CharacterTabType.entries[tabUiState.selectedTabIndex]) {
        CharacterTabType.Category -> {
            // Category 탭: 기존 LazyColumn 구조 유지
            androidx.compose.foundation.lazy.LazyColumn(modifier = modifier) {
                item {
                    CharacterShopTabRow(
                        selectedTabIndex = tabUiState.selectedTabIndex,
                        onTabSelected = onTabSelected,
                    )
                }
                item {
                    CharacterCategorySection()
                }
            }
        }

        CharacterTabType.Shop -> {
            // Shop 탭: BottomSheetScaffold를 위해 전체 화면 사용
            // 탭 row는 scaffold content 안에 포함
            CharacterShopRoute(
                modifier = modifier,
                selectedTabIndex = tabUiState.selectedTabIndex,
                onTabSelected = onTabSelected
            )
        }
    }
}