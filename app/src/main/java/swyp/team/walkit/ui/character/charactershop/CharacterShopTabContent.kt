package swyp.team.walkit.ui.character.charactershop

import androidx.compose.runtime.Composable
import swyp.team.walkit.ui.character.component.CharacterCategorySection

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
            CharacterShopRoute()
        }
    }
}


