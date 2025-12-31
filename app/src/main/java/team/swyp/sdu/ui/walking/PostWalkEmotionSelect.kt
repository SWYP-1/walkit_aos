package team.swyp.sdu.ui.walking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.EmotionSlider
import team.swyp.sdu.ui.components.SectionCard
import team.swyp.sdu.ui.components.TextHighlight
import team.swyp.sdu.ui.components.WalkingWarningDialog
import team.swyp.sdu.ui.walking.utils.createDefaultEmotionOptions
import team.swyp.sdu.ui.walking.utils.findSelectedEmotionIndex
import team.swyp.sdu.ui.walking.utils.valueToEmotionType
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.ui.walking.components.WalkingProgressBar
import timber.log.Timber

/**
 * 산책 후 감정 선택 Route
 * ViewModel injection과 state collection을 담당하는 Route composable
 */
@Composable
fun PostWalkingEmotionSelectRoute(
    viewModel: WalkingViewModel,
    onNext: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // ViewModel 인스턴스 확인 로그
    LaunchedEffect(Unit) {
        Timber.d("🚶 PostWalkingEmotionSelectRoute - 진입: viewModel.hashCode=${viewModel.hashCode()}, currentSessionLocalId=${viewModel.currentSessionLocalIdValue}")
    }

    val selectedEmotion by viewModel.postWalkingEmotion.collectAsStateWithLifecycle()

    PostWalkingEmotionSelectScreen(
        selectedEmotion = selectedEmotion,
        onEmotionSelected = viewModel::selectPostWalkingEmotion,
        onNextClick = {
            if (selectedEmotion != null) {
                viewModel.updatePostWalkEmotion(selectedEmotion!!)
                onNext()
            }
        },
        onClose = {
            // 임시로 저장된 산책 기록 삭제
            viewModel.deleteCurrentSession()
            onClose()
        },
        modifier = modifier,
    )
}

/**
 * 산책 후 감정 선택 Screen
 * UI 컴포넌트로 state와 callbacks를 파라미터로 받음
 */
@Composable
private fun PostWalkingEmotionSelectScreen(
    selectedEmotion: EmotionType?,
    onEmotionSelected: (EmotionType) -> Unit,
    onNextClick: () -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showWarningDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 헤더 (닫기 버튼)
            AppHeader(
                title = "",
                onNavigateBack = {
                    showWarningDialog = true
                },
            )

            // 진행 바 (1번째 칸 채워짐)
            WalkingProgressBar(
                currentStep = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
            SectionCard {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "산책 후 나의 마음은 어떤가요?",
                        style = MaterialTheme.walkItTypography.headingS.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "산책 후 감정이 어떻게 변했는지 기록해주세요",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {


                // 감정 옵션 리스트 생성
                val emotionOptions = remember {
                    createDefaultEmotionOptions()
                }

                // 선택된 감정의 인덱스 찾기
                val selectedIndex = findSelectedEmotionIndex(selectedEmotion, emotionOptions)

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
                        text = "닫기",
                        textColor = SemanticColor.buttonPrimaryDefault,
                        buttonColor = SemanticColor.backgroundWhitePrimary,
                        onClick = {
                            showWarningDialog = true
                        },
                        modifier = Modifier.width(96.dp)
                    )

                    CtaButton(
                        text = "다음으로",
                        textColor = SemanticColor.textBorderPrimaryInverse,
                        onClick = onNextClick,
                        enabled = selectedEmotion != null,
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_forward),
                                contentDescription = "arrow forward",
                                tint = SemanticColor.iconWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }

            // 경고 다이얼로그
            if (showWarningDialog) {
                WalkingWarningDialog(
                    title = "산책 기록을 중단하시겠습니까?",
                    message = "이대로 종료하시면 작성한 산책 기록이\n모두 사라져요!",
                    titleHighlight = TextHighlight(
                        text = "중단",
                        color = SemanticColor.stateRedPrimary,
                    ),
                    cancelButtonText = "중단하기",
                    continueButtonText = "계속하기",
                    onDismiss = { showWarningDialog = false },
                    onCancel = {
                        // 산책 기록 중단 및 메인 화면으로 이동
                        showWarningDialog = false
                        onClose()
                    },
                    onContinue = {
                        // 다이얼로그만 닫기
                        showWarningDialog = false
                    },
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun PostWalkingEmotionSelectScreenPreview() {
    WalkItTheme {
        PostWalkingEmotionSelectScreen(
            selectedEmotion = EmotionType.CONTENT,
            onEmotionSelected = {},
            onNextClick = {},
            onClose = {},
        )
    }
}