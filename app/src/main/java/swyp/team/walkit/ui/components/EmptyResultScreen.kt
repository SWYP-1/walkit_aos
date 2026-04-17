package swyp.team.walkit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 검색/목록 결과가 없을 때 공통으로 사용하는 빈 화면 컴포넌트.
 *
 * @param title    굵은 메인 메시지 (예: "검색 결과가 없어요")
 * @param subtitle 보조 안내 문구 (예: "다른 검색어를 입력하세요")
 * @param modifier 외부에서 주입하는 Modifier
 */
@Composable
fun EmptyResultScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = SemanticColor.backgroundWhiteSecondary,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 40.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_face_search_empty),
                contentDescription = null,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.walkItTypography.bodyXL.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = SemanticColor.textBorderPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = SemanticColor.textBorderSecondary,
            )
        }
    }
}
