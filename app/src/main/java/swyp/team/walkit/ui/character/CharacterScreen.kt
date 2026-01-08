package swyp.team.walkit.ui.character

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import swyp.team.walkit.ui.character.charactershop.CharacterShopRoute



/**
 * 캐릭터 상점 Screen
 *
 * 아이템 상점만 표시 (탭 구조 제거)
 */
@Composable
fun CharacterScreen(
    modifier: Modifier = Modifier,
) {
    // 아이템 상점만 표시
    CharacterShopRoute(
        modifier = modifier
    )
}