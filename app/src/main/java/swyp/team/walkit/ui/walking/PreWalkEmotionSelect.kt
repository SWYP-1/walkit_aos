package swyp.team.walkit.ui.walking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.CtaButtonVariant
import swyp.team.walkit.ui.components.PreviousButton
import swyp.team.walkit.ui.components.EmotionSlider
import swyp.team.walkit.ui.components.SectionCard
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.ui.walking.utils.createDefaultEmotionOptions
import swyp.team.walkit.ui.walking.utils.findSelectedEmotionIndex
import swyp.team.walkit.ui.walking.utils.valueToEmotionType
import swyp.team.walkit.ui.walking.viewmodel.WalkingUiState
import swyp.team.walkit.ui.walking.viewmodel.WalkingViewModel
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
    val scope = rememberCoroutineScope()
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
        onPrev = {
            // 산책 준비 단계에서 뒤로가기 시 서비스 중단 확인
            scope.launch {
                try {
                    // 만약 서비스가 시작되었다면 중단 (완료될 때까지 대기)
                    viewModel.stopWalkingIfNeeded()
                    Timber.d("🚶 PreWalkingEmotionSelect - 서비스 중단 확인 완료")
                } catch (e: Throwable) {
                    Timber.e(e, "🚶 PreWalkingEmotionSelect - 서비스 중단 실패")
                } finally {
                    // 서비스 중단 완료 후 네비게이션 실행
                    onPrev()
                }
            }
        },
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

    // 시스템 백 버튼 처리 (서비스 중단 후 뒤로가기)
    BackHandler {
        onPrev()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()      // ⭐ 1. 시스템 영역 회피
            .verticalScroll(rememberScrollState()) // ⭐ 2. 콘텐츠 스크롤
            .background(SemanticColor.backgroundWhitePrimary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
            SectionCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp),
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

        }

        // 🔹 하단 고정 영역

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviousButton(
                onClick = onPrev
            )
            Box(
                modifier = Modifier.weight(1f)
            ) {
                CtaButton(
                    text = "다음으로",
                    onClick = onNext,
                    enabled = true,
                    iconResId = R.drawable.ic_arrow_forward,
                    modifier = Modifier.fillMaxWidth()
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