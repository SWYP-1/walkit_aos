package team.swyp.sdu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.LottieCharacterState
import team.swyp.sdu.ui.theme.WalkItTheme
import timber.log.Timber

/**
 * 범용 캐릭터 표시용 Lottie 컴포넌트
 *
 * 캐릭터 데이터를 받아서 head/body/feet 파트를 Lottie JSON asset으로 교체하여 표시합니다.
 * null이나 빈 값인 파트는 투명 PNG로 대체됩니다.
 *
 * @param characterLottieState 캐릭터 Lottie 상태 데이터
 * @param modifier Modifier
 * @param size 표시할 캐릭터 크기 (dp)
 * @param defaultAnimationResId 기본 애니메이션 리소스 ID (데이터가 없을 때 표시)
 */
@Composable
fun LottieCharacterDisplay(
    characterLottieState: LottieCharacterState?,
    modifier: Modifier = Modifier,
    size: Int = 200,
    defaultAnimationResId: Int = R.raw.seedblueribbon
) {
    Timber.d("🎭 LottieCharacterDisplay 렌더링: state=${characterLottieState?.isLoading}, hasModified=${characterLottieState?.modifiedJson != null}")

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 로딩 중
            characterLottieState?.isLoading == true -> {
                Timber.d("⏳ Lottie 로딩 중 표시")
                LottieAnimation(
                    composition = rememberLottieComposition(
                        LottieCompositionSpec.RawRes(defaultAnimationResId)
                    ).value,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 에러 상태
            characterLottieState?.error != null -> {
                Timber.e("❌ Lottie 에러 상태: ${characterLottieState.error}")
                LottieAnimation(
                    composition = rememberLottieComposition(
                        LottieCompositionSpec.RawRes(defaultAnimationResId)
                    ).value,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 수정된 JSON이 있는 경우 (캐릭터 파트 교체된 상태)
            characterLottieState?.modifiedJson != null -> {
                Timber.d("✅ 수정된 Lottie JSON으로 애니메이션 표시")
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.JsonString(characterLottieState.modifiedJson)
                )

                composition?.let {
                    LottieAnimation(
                        composition = it,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 기본 상태 (수정 전 base JSON)
            characterLottieState?.baseJson != null -> {
                Timber.d("🎨 기본 Lottie JSON으로 애니메이션 표시")
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.JsonString(characterLottieState.baseJson)
                )

                composition?.let {
                    LottieAnimation(
                        composition = it,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 초기 상태 (아직 데이터가 없는 경우)
            else -> {
                Timber.d("🎯 초기 상태 - 기본 Lottie 표시")
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(defaultAnimationResId)
                )

                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LottieCharacterDisplayPreview() {
    WalkItTheme {
        // 로딩 상태 미리보기
        LottieCharacterDisplay(
            characterLottieState = LottieCharacterState(
                baseJson = "{}",
                modifiedJson = null,
                assets = emptyMap(),
                isLoading = true
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LottieCharacterDisplayWithDataPreview() {
    WalkItTheme {
        // 데이터 있는 상태 미리보기
        LottieCharacterDisplay(
            characterLottieState = LottieCharacterState(
                baseJson = "{}",
                modifiedJson = "{}", // 실제로는 수정된 JSON
                assets = mapOf(
                    "head_asset" to team.swyp.sdu.domain.model.LottieAsset("head_asset"),
                    "body_asset" to team.swyp.sdu.domain.model.LottieAsset("body_asset"),
                    "feet_asset" to team.swyp.sdu.domain.model.LottieAsset("feet_asset")
                ),
                isLoading = false
            )
        )
    }
}
