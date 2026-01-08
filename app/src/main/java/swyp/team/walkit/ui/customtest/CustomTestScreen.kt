package swyp.team.walkit.ui.customtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showSuccessMessage by remember { mutableStateOf(false) }

    // 데이터 추가 성공 시 스낵바 표시
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "더미 데이터 40개가 성공적으로 추가되었습니다!\n홈 화면이나 산책 기록 화면에서 확인해주세요.\n\n💡 표시되지 않으면 화면을 아래로 당겨 새로고침해보세요.",
                    actionLabel = "확인"
                )
            }
            showSuccessMessage = false
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
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
                onClick = {
                    onAddDummySessions()
                    showSuccessMessage = true
                },
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

        // 스낵바 호스트
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomTestScreenPreview() {
    WalkItTheme {
        CustomTestScreen()
    }
}
