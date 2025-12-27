package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import team.swyp.sdu.ui.theme.SemanticColor

/**
 * 재사용 가능한 로딩 오버레이 컴포넌트
 *
 * API 호출 중 로딩 상태를 표시하는 오버레이입니다.
 * 반투명 배경 위에 CustomProgressIndicator를 표시합니다.
 *
 * @param isLoading 로딩 중인지 여부
 * @param backgroundColor 배경색 (기본값: 반투명 검정)
 * @param modifier Modifier
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            CustomProgressIndicator(
                size = ProgressIndicatorSize.Medium,
            )
        }
    }
}





