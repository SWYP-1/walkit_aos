package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme

/**
 * Lottie 기반 로딩 인디케이터
 *
 * 기존 CustomProgressIndicator의 Small / Medium size 옵션 유지
 * 중앙에 Lottie JSON 애니메이션 재생
 */
@Composable
fun CustomProgressIndicator(
    size: ProgressIndicatorSize = ProgressIndicatorSize.Small,
    modifier: Modifier = Modifier
) {
    val indicatorSize = when (size) {
        ProgressIndicatorSize.Small -> 36.dp
        ProgressIndicatorSize.Medium -> 40.dp
    }

    // Lottie Composition 로드
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_gray))

    // 최신 방식: LottieAnimationState 생성
    val lottieState = rememberLottieAnimatable()
    LaunchedEffect(composition) {
        composition?.let { lottieState.animate(it, iterations = LottieConstants.IterateForever) }
    }

    Box(
        modifier = modifier.size(indicatorSize),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { lottieState.progress },
            modifier = Modifier.size(indicatorSize)
        )
    }
}

/**
 * 프로그레스 인디케이터 크기
 */
enum class ProgressIndicatorSize {
    Small,
    Medium,
}

/**
 * Preview
 */
@Composable
fun CustomProgressIndicatorPreview() {
    WalkItTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(SemanticColor.backgroundWhiteSecondary),
            contentAlignment = Alignment.Center,
        ) {
            CustomProgressIndicator(size = ProgressIndicatorSize.Small)
        }
    }
}

@Composable
fun CustomProgressIndicatorMediumPreview() {
    WalkItTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(SemanticColor.backgroundWhiteSecondary),
            contentAlignment = Alignment.Center,
        ) {
            CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
        }
    }
}
