package team.swyp.sdu.ui.customtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 커스텀 테스트 화면
 *
 * 온보딩 실행을 위한 테스트 화면입니다.
 */
@Composable
fun CustomTestScreen(
    onNavigateBack: () -> Unit = {},
    onStartOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 헤더
        AppHeader(
            title = "커스텀 테스트",
            onNavigateBack = onNavigateBack,
        )

        // 콘텐츠
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "개발자용 테스트 메뉴",
                style = MaterialTheme.walkItTypography.headingM,
            )

            Text(
                text = "온보딩 플로우를 테스트할 수 있습니다.",
                style = MaterialTheme.walkItTypography.bodyM,
            )
        }

        // 온보딩 시작 버튼
        CtaButton(
            text = "온보딩으로 가기",
            onClick = onStartOnboarding,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomTestScreenPreview() {
    WalkItTheme {
        CustomTestScreen()
    }
}
