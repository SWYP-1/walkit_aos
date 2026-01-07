package swyp.team.walkit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.LottieCharacterState
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.components.ProgressIndicatorSize
import swyp.team.walkit.ui.theme.WalkItTheme
import timber.log.Timber

/**
 * WalkIt 앱의 Lottie 애니메이션 리소스 상수
 */
// Lottie 리소스 상수들
val LOTTIE_DEFAULT_CHARACTER = R.raw.seed
val LOTTIE_LOADING = R.raw.loading_gray
val LOTTIE_ERROR = R.raw.seed

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
    defaultAnimationResId: Int = LOTTIE_DEFAULT_CHARACTER
) {
    Timber.d("🎭 LottieCharacterDisplay 렌더링: state=${characterLottieState?.isLoading}, hasModified=${characterLottieState?.modifiedJson != null}")

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // 초기 로딩 상태 확인 (데이터가 전혀 없을 때만)
        val isInitialLoading = characterLottieState == null ||
            (characterLottieState.baseJson == null && characterLottieState.modifiedJson == null)

        // 아이템 교체 중인지 확인 (기존 캐릭터가 있고 로딩 중)
        val isItemReplacing = characterLottieState?.isLoading == true &&
            (characterLottieState.modifiedJson != null || characterLottieState.baseJson != null)

        if (isInitialLoading) {
            // 초기 로딩 (데이터가 전혀 없을 때) → CustomProgressIndicator 표시
            CustomProgressIndicator(
                size = ProgressIndicatorSize.Medium
            )
        } else {
            // 아이템 교체 중이거나 로딩 완료 상태 → Lottie 애니메이션 표시
            // 아이템 교체 중일 때는 기존 캐릭터를 유지하여 깜빡임 방지
            val compositionSpec = getLottieCompositionSpec(characterLottieState, defaultAnimationResId)

            // ✅ Composition 상태 관리 (깜빡임 방지)
            var currentComposition by remember { mutableStateOf<LottieComposition?>(null) }

            // 새로운 composition 로드
            val newComposition by rememberLottieComposition(
                spec = compositionSpec,
            )

            // 새로운 composition이 준비되면 즉시 교체 (기존 composition 유지)
            LaunchedEffect(newComposition) {
                newComposition?.let { composition ->
                    currentComposition = composition
                }
            }

            // 현재 composition이 있으면 표시, 없으면 기본 애니메이션
            currentComposition?.let { composition ->
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                // 초기 로딩 시 기본 애니메이션 표시
                val fallbackComposition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(defaultAnimationResId)
                )
                LottieAnimation(
                    composition = fallbackComposition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 캐릭터 상태에 따른 Lottie CompositionSpec 결정
 * 깜빡임 방지를 위해 로딩 상태 제거하고 기존 composition 유지
 */
@Composable
private fun getLottieCompositionSpec(
    characterLottieState: LottieCharacterState?,
    defaultAnimationResId: Int
): LottieCompositionSpec {
    return when {
        // ✅ 로딩 중에도 기존 composition 유지 (깜빡임 방지)
        characterLottieState?.isLoading == true -> {
            Timber.d("⏳ Lottie 로딩 중 - 기존 composition 유지")
            // 로딩 중에는 마지막으로 유효했던 composition 사용
            characterLottieState.modifiedJson?.let { LottieCompositionSpec.JsonString(it) }
                ?: (characterLottieState.baseJson?.let { LottieCompositionSpec.JsonString(it) })
                ?: LottieCompositionSpec.RawRes(defaultAnimationResId)
        }

        // 에러 상태
        characterLottieState?.error != null -> {
            Timber.e("❌ Lottie 에러 상태: ${characterLottieState.error}")
            LottieCompositionSpec.RawRes(LOTTIE_ERROR)
        }

        // 수정된 JSON이 있는 경우 (캐릭터 파트 교체된 상태)
        characterLottieState?.modifiedJson != null -> {
            Timber.d("✅ 수정된 Lottie JSON으로 애니메이션 표시")
            LottieCompositionSpec.JsonString(characterLottieState.modifiedJson)
        }

        // 기본 상태 (수정 전 base JSON)
        characterLottieState?.baseJson != null -> {
            Timber.d("🎨 기본 Lottie JSON으로 애니메이션 표시")
            LottieCompositionSpec.JsonString(characterLottieState.baseJson)
        }

        // 초기 상태 (아직 데이터가 없는 경우)
        else -> {
            Timber.d("🎯 초기 상태 - 기본 Lottie 표시")
            LottieCompositionSpec.RawRes(defaultAnimationResId)
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
                    "head_asset" to swyp.team.walkit.domain.model.LottieAsset("head_asset"),
                    "body_asset" to swyp.team.walkit.domain.model.LottieAsset("body_asset"),
                    "feet_asset" to swyp.team.walkit.domain.model.LottieAsset("feet_asset")
                ),
                isLoading = false
            )
        )
    }
}

