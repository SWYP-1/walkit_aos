package team.swyp.sdu.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.LoadingOverlay
import team.swyp.sdu.ui.onboarding.component.OnBoardingStepTag
import team.swyp.sdu.ui.theme.Black
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 닉네임 입력 관련 상수들
 */
private object NicknameStepConstants {
    const val MAX_LENGTH = 20
    const val NICKNAME_STEP_INDEX = 0
}

/**
 * 닉네임 입력 단계의 UI 상태 계산
 */
private data class NicknameStepUiState(
    val isLoading: Boolean,
    val isError: Boolean,
    val errorMessage: String?,
    val canGoNext: Boolean,
    val maxLength: Int
)

private fun calculateNicknameStepUiState(uiState: OnboardingUiState): NicknameStepUiState {
    val isLoading = uiState.isLoading && uiState.currentStep == NicknameStepConstants.NICKNAME_STEP_INDEX
    val nicknameState = uiState.nicknameState

    val isError = nicknameState.isDuplicate == true || nicknameState.validationError != null
    val errorMessage = when {
        nicknameState.isDuplicate == true -> "중복된 닉네임입니다."
        nicknameState.validationError != null -> nicknameState.validationError
        else -> null
    }
    val canGoNext = nicknameState.value.isNotBlank() &&
            nicknameState.isDuplicate == false &&
            nicknameState.validationError == null

    return NicknameStepUiState(
        isLoading = isLoading,
        isError = isError,
        errorMessage = errorMessage,
        canGoNext = canGoNext,
        maxLength = NicknameStepConstants.MAX_LENGTH
    )
}

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
    val stepUiState = calculateNicknameStepUiState(uiState)

    // 키보드 상태 관리
    val (isImeVisible, safeKeyboardController, focusRequester) = rememberKeyboardState()

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
                    text = "닉네임을 만들어주세요",
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
                            safeKeyboardController.show()
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
                                // 키보드 내리기
                                safeKeyboardController.hide()
                                // 유효성 검증 통과 시 중복 체크 수행
                                if (stepUiState.canGoNext) {
                                    onCheckDuplicate()
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
                if (stepUiState.isError && stepUiState.errorMessage != null) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stepUiState.errorMessage!!,
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
                        iconResId = R.drawable.ic_arrow_forward
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // API 요청 시 로딩 오버레이 표시
            LoadingOverlay(isLoading = stepUiState.isLoading)
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun NicknameStepFilledPreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(
                    value = "닉네임예시",
                    validationError = null // 유효성 검증 통과 상태
                ),
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
                nicknameState = NicknameState(
                    value = "",
                    validationError = null // 빈 문자열은 에러 표시하지 않음
                ),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "유효하지 않은 닉네임")
@Composable
private fun NicknameStepInvalidPreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(
                    value = "닉네임@123", // 특수문자 포함
                    validationError = "닉네임은 한글과 영문(대소문자)만 사용할 수 있습니다"
                ),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "중복 닉네임")
@Composable
private fun NicknameStepDuplicatePreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(
                    value = "중복닉네임",
                    isDuplicate = true,
                    validationError = null
                ),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "긴 닉네임 (21자)")
@Composable
private fun NicknameStepTooLongPreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(
                    value = "매우매우매우매우매우긴닉네임", // 21자
                    validationError = "닉네임은 최대 20자까지 입력 가능합니다"
                ),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "띄어쓰기 포함 닉네임")
@Composable
private fun NicknameStepWithSpacePreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(
                    value = "닉네 임 예시",
                    validationError = "닉네임에 띄어쓰기를 사용할 수 없습니다"
                ),
            ),
            onCheckDuplicate = {},
            onNext = {},
            onNicknameChange = {},
            onPrev = {},
        )
    }
}

/**
 * 키보드 상태를 관리하는 헬퍼 함수
 * Preview 안전성을 위해 null 처리를 포함
 */
@Composable
private fun rememberKeyboardState(): Triple<Boolean, SoftwareKeyboardController, FocusRequester> {
    val density = LocalDensity.current
    val imePadding = WindowInsets.ime.asPaddingValues()
    val isImeVisible = imePadding.calculateBottomPadding().value > 0
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Preview에서 composition local들이 null일 수 있으므로 기본 구현 제공
    val safeKeyboardController = keyboardController ?: object : SoftwareKeyboardController {
        override fun hide() {}
        override fun show() {}
    }

    return Triple(isImeVisible, safeKeyboardController, focusRequester)
}