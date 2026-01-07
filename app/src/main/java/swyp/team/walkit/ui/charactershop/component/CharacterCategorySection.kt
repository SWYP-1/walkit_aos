package swyp.team.walkit.ui.charactershop.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 캐릭터 카테고리 섹션 컴포넌트
 * 캐릭터 카테고리 탭의 콘텐츠를 표시합니다.
 */
@Composable
fun CharacterCategorySection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Spacer(Modifier.height(24.dp))

        // 카테고리 헤더
        Text(
            text = "캐릭터 카테고리",
            style = MaterialTheme.walkItTypography.headingM,
            color = SemanticColor.textBorderPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 긴 콘텐츠 예시 (실제로는 카테고리별 캐릭터 리스트 등)
        repeat(20) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = if (index % 2 == 0) SemanticColor.backgroundWhiteTertiary else androidx.compose.ui.graphics.Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "캐릭터 카테고리 ${index + 1}",
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "이 카테고리의 캐릭터 설명이 여기에 표시됩니다. 실제로는 캐릭터 이미지와 상세 정보가 포함됩니다.",
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = SemanticColor.textBorderSecondary
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
