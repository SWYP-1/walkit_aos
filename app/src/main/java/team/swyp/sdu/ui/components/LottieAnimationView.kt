package team.swyp.sdu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import team.swyp.sdu.R
import timber.log.Timber

/**
 * 범용 Lottie 애니메이션 뷰 컴포넌트
 *
 * @param animationResId 표시할 Lottie 애니메이션 리소스 ID
 * @param isPlaying 재생 중인지 여부
 * @param modifier Modifier
 * @param size 애니메이션 크기 (기본값: 200.dp)
 * @param speed 재생 속도 (기본값: 1.0f)
 * @param iterations 반복 횟수 (기본값: 무한 반복)
 */
@Composable
fun LottieAnimationView(
    animationResId: Int = R.raw.walking_avocado,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    isPlaying: Boolean = true,
    speed: Float = 1f,
    iterations: Int = LottieConstants.IterateForever
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(animationResId)
    )

    val anim = rememberLottieAnimatable()

    LaunchedEffect(composition, isPlaying) {
        if (composition == null) return@LaunchedEffect

        if (isPlaying) {
            anim.animate(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = speed,
                initialProgress = anim.progress, // 현재 위치에서 이어서 시작
                continueFromPreviousAnimate = true
            )
        } else {
            // ⭐ 재생을 멈추고 progress 유지
            anim.snapTo(
                composition = composition,
                progress = anim.progress
            )
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { anim.progress },
        modifier = modifier.size(size)
    )
}


