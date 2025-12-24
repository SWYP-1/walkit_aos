package team.swyp.sdu.ui.mypage.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.components.CtaButton

/**
 * 마이 페이지 캐릭터 정보 수정 버튼 컴포넌트
 */
@Composable
fun MyPageCharacterEditButton(
    onNavigateCharacterEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CtaButton(
        text = "캐릭터 정보 수정",
        onClick = onNavigateCharacterEdit,
        modifier = modifier.padding(horizontal = 50.dp)
    )
}





