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
    onAddDummySessions: () -> Unit = {},
    onNavigateToMapTest: () -> Unit = {},
    onNavigateToGalleryTest: () -> Unit = {},
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "개발자용 테스트 메뉴",
                style = MaterialTheme.walkItTypography.headingM,
            )

            Text(
                text = "온보딩 플로우와 더미 데이터를 테스트할 수 있습니다.",
                style = MaterialTheme.walkItTypography.bodyM,
            )
        }

        // 더미 세션 추가 버튼
        CtaButton(
            text = "더미 세션 데이터 추가 (40개)",
            onClick = onAddDummySessions,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 카카오 맵뷰 테스트 버튼
        CtaButton(
            text = "카카오 맵뷰 테스트",
            onClick = onNavigateToMapTest,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 갤러리 사진 테스트 버튼
        CtaButton(
            text = "갤러리 사진 + 경로 테스트",
            onClick = onNavigateToGalleryTest,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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
