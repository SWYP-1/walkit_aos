package team.swyp.sdu.ui.walking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.CtaButtonVariant
import team.swyp.sdu.ui.components.EmotionSlider
import team.swyp.sdu.ui.components.SectionCard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.ui.walking.utils.createDefaultEmotionOptions
import team.swyp.sdu.ui.walking.utils.findSelectedEmotionIndex
import team.swyp.sdu.ui.walking.utils.valueToEmotionType
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import timber.log.Timber


/**
 * 산책 전 감정 선택 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun PreWalkingEmotionSelectRoute(
    viewModel: WalkingViewModel = hiltViewModel(),
    onPrev: () -> Unit,
    onNext: () -> Unit,
    permissionsGranted: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedEmotion = when (uiState) {
        is WalkingUiState.PreWalkingEmotionSelection -> (uiState as WalkingUiState.PreWalkingEmotionSelection).preWalkingEmotion
        else -> null
    }

    // 사용자 로그인 상태 확인
    LaunchedEffect(Unit) {
        try {
            val userId = viewModel.getCurrentUserId()
            if (userId == 0L) {
                Timber.w("로그인되지 않은 사용자가 산책을 시도함")
                // 로그인 필요 메시지를 표시하거나 이전 화면으로 돌아감
                onPrev()
            }
        } catch (t: Throwable) {
            Timber.e(t, "사용자 상태 확인 실패")
            onPrev()
        }
    }

    PreWalkingEmotionSelectScreen(
        selectedEmotion = selectedEmotion,
        permissionsGranted = permissionsGranted,
        onEmotionSelected = viewModel::selectPreWalkingEmotion,
        onPrev = onPrev,
        onNext = onNext,
    )
}
/**
 * 산책 전 감정 선택 화면
 *
 * UI 컴포넌트로 상태와 콜백을 파라미터로 받습니다.
 */
@Composable
fun PreWalkingEmotionSelectScreen(
    selectedEmotion: EmotionType?,
    permissionsGranted: Boolean,
    onEmotionSelected: (EmotionType) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 감정 옵션 리스트 생성 (Composable 함수 사용)
    val emotionOptions = createDefaultEmotionOptions()

    // 선택된 감정의 인덱스 찾기
    val selectedIndex = findSelectedEmotionIndex(selectedEmotion, emotionOptions)

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(SemanticColor.backgroundWhitePrimary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        AppHeader(title = "", showBackButton = true, onNavigateBack = onPrev)
        Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
            SectionCard {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "산책 전 나의 마음은 어떤가요?",
                        style = MaterialTheme.walkItTypography.headingS.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "산책하기 전 지금 어떤 감정을 느끼는지 선택해주세요",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

            }
            Spacer(Modifier.height(40.dp))

            // EmotionSlider를 사용한 감정 선택
            EmotionSlider(
                modifier = Modifier.fillMaxWidth(),
                emotions = emotionOptions,
                selectedIndex = selectedIndex,
                onEmotionSelected = { index ->
                    if (index in emotionOptions.indices) {
                        val emotionType = valueToEmotionType(emotionOptions[index].value)
                        onEmotionSelected(emotionType)
                    }
                }
            )
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CtaButton(
                    text = "이전으로",
                    variant = CtaButtonVariant.SECONDARY,
                    onClick = onPrev,
                    modifier = Modifier.width(96.dp)
                )

                CtaButton(
                    text = "다음으로",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    enabled = permissionsGranted, // 권한이 없으면 버튼 비활성화
                    iconResId = R.drawable.ic_arrow_forward
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreWalkingEmotionSelectScreenPreview_NoSelection() {
    WalkItTheme {
        PreWalkingEmotionSelectScreen(
            selectedEmotion = null,
            permissionsGranted = true,
            onEmotionSelected = {},
            onPrev = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreWalkingEmotionSelectScreenPreview_WithSelection() {
    WalkItTheme {
        PreWalkingEmotionSelectScreen(
            selectedEmotion = EmotionType.HAPPY,
            permissionsGranted = true,
            onEmotionSelected = {},
            onPrev = {},
            onNext = {},
        )
    }
}