package team.swyp.sdu.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.WalkItTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import team.swyp.sdu.R
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.LoadingOverlay
import team.swyp.sdu.ui.onboarding.component.OnBoardingStepTag
import team.swyp.sdu.ui.theme.Black
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 닉네임 입력 단계 컴포넌트
 */
@Composable
fun NicknameStep(
    uiState: OnboardingUiState,
    onNicknameChange: (String) -> Unit,
    onCheckDuplicate: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    // 닉네임 등록 API 호출 시 로딩 표시
    val isLoading = uiState.isLoading && uiState.currentStep == 0
    val nicknameState = uiState.nicknameState
    val maxLength = 20

    val isError = nicknameState.isDuplicate == true
    val canGoNext =
        nicknameState.value.isNotBlank() &&
                nicknameState.isDuplicate == false

    // 키보드가 올라왔는지 확인 (키보드 높이가 0보다 크면 올라온 것으로 판단)
    val density = LocalDensity.current
    val imePadding = WindowInsets.ime.asPaddingValues()
    val isImeVisible = imePadding.calculateBottomPadding().value > 0
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhiteSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AppHeader(
                "",
                showBackButton = false,
                background = SemanticColor.backgroundWhiteSecondary
            ) {
//            Icon(
//                painter = painterResource(R.drawable.ic_action_clear),
//                contentDescription = null,
//                tint = SemanticColor.iconBlack,
//                modifier = Modifier.size(24.dp),
//            )
            }

            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                OnBoardingStepTag(text = "준비 단계")

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "캐릭터의 이름을 만들어주세요",
                    style = MaterialTheme.walkItTypography.headingM.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Black
                )

                Spacer(Modifier.height(64.dp))

                Image(
                    painter = painterResource(R.drawable.walk_it_character),
                    contentDescription = "walkit character"
                )

                // 키보드가 올라왔을 때는 Spacer 높이를 줄이고, 평상시에는 92.dp 유지
                Spacer(
                    modifier = Modifier.height(
                        if (isImeVisible) 16.dp else 92.dp
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clickable {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .border(
                            width = 1.dp,
                            color = if (isError) SemanticColor.stateRedPrimary
                            else SemanticColor.textBorderPrimary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = nicknameState.value,
                        onValueChange = onNicknameChange,
                        singleLine = true,
                        textStyle = MaterialTheme.walkItTypography.bodyM,
                        modifier = Modifier.focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // 항상 키보드 내리기
                                keyboardController?.hide()
                                // 다음 단계로 진행 가능한 경우에만 진행
                                if (canGoNext) {
                                    onNext()
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (nicknameState.value.isEmpty()) {
                                Text(
                                    "닉네임을 입력해주세요",
                                    style = MaterialTheme.walkItTypography.bodyM,
                                    color = SemanticColor.textBorderSecondary
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                if (isError) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "중복된 닉네임입니다.",
                            style = MaterialTheme.walkItTypography.bodyS.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SemanticColor.stateRedPrimary,
                        )
                    }

                }


                Spacer(modifier = Modifier.weight(1f))

                if (!isImeVisible) {
                    CtaButton(
                        text = "다음으로",
                        enabled = true,
                        onClick = onNext,
                        buttonColor = SemanticColor.buttonPrimaryDefault,
                        textColor = SemanticColor.textBorderPrimaryInverse
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = null,
                            tint = SemanticColor.iconWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // API 요청 시 로딩 오버레이 표시
            LoadingOverlay(isLoading = isLoading)
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun NicknameStepFilledPreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(value = "닉네임예시"),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NicknameStepEmptyPreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(value = ""),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}


