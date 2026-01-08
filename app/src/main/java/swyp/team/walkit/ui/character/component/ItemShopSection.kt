package swyp.team.walkit.ui.character.charactershop.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 아이템 상점 섹션 컴포넌트
 * 아이템 상점 탭의 그리드 콘텐츠를 표시합니다.
 */
@Composable
fun ItemShopSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Spacer(Modifier.height(24.dp))

        // 상점 헤더
        Text(
            text = "아이템 상점",
            style = MaterialTheme.walkItTypography.headingM,
            color = SemanticColor.textBorderPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 그리드 리스트 예시
        val items = List(12) { "아이템 ${it + 1}" }
        val rows = items.chunked(3) // 3열씩 그룹화

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    color = SemanticColor.backgroundWhiteTertiary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = SemanticColor.textBorderTertiary,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.walkItTypography.captionM,
                                color = SemanticColor.textBorderPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // 빈 칸 채우기 (3열 유지)
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
