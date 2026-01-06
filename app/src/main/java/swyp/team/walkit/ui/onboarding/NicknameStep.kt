package swyp.team.walkit.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swyp.team.walkit.R
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.LoadingOverlay
import swyp.team.walkit.ui.onboarding.NicknameStepConstants.MAX_LENGTH
import swyp.team.walkit.ui.onboarding.component.OnBoardingStepTag
import swyp.team.walkit.ui.theme.Black
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/* ---------------------------------------------
 * Constants
 * --------------------------------------------- */

private object NicknameStepConstants {
    const val MAX_LENGTH = 20
    const val NICKNAME_STEP_INDEX = 0
}

/* ---------------------------------------------
 * UI State
 * --------------------------------------------- */

private data class NicknameStepUiState(
    val isLoading: Boolean,
    val isError: Boolean,
    val errorMessage: String?,
    val canGoNext: Boolean,
)

/* ---------------------------------------------
 * UI State Mapper
 * --------------------------------------------- */

private fun calculateNicknameStepUiState(uiState: OnboardingUiState): NicknameStepUiState {
    val nicknameState = uiState.nicknameState

    val isError = nicknameState.isDuplicate == true || nicknameState.validationError != null
    val errorMessage = when {
        nicknameState.isDuplicate == true -> "중복된 닉네임입니다."
        nicknameState.validationError != null -> nicknameState.validationError
        else -> null
    }

    val canGoNext =
        nicknameState.value.isNotBlank() &&
                nicknameState.validationError == null &&
                (nicknameState.isDuplicate == false || nicknameState.isDuplicate == null)


    return NicknameStepUiState(
        isLoading = uiState.isLoading && uiState.currentStep == NicknameStepConstants.NICKNAME_STEP_INDEX,
        isError = isError,
        errorMessage = errorMessage,
        canGoNext = canGoNext
    )
}

/* ---------------------------------------------
 * Step Root
 * --------------------------------------------- */

@Composable
fun NicknameStep(
    uiState: OnboardingUiState,
    onNicknameChange: (String) -> Unit,
    onCheckDuplicate: () -> Unit,
    onNext: () -> Unit,
) {
    val stepUiState = calculateNicknameStepUiState(uiState)
    val (isImeVisible, keyboardController, focusRequester) = rememberKeyboardState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhiteSecondary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AppHeader(
                title = "",
                showBackButton = false,
                background = SemanticColor.backgroundWhiteSecondary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    contentDescription = null
                )

                Spacer(
                    Modifier.height(
                        if (isImeVisible) 16.dp else 92.dp
                    )
                )

                NicknameInputField(
                    value = uiState.nicknameState.value,
                    isError = stepUiState.isError,
                    errorMessage = stepUiState.errorMessage,
                    focusRequester = focusRequester,
                    keyboardController = keyboardController,
                    onValueChange = onNicknameChange,
                    onDone = {
                        keyboardController.hide()
//                        if (stepUiState.canGoNext) {
//                            onCheckDuplicate()
//                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f, fill = true))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding() // 🔥 여기만
                        .padding(bottom = 24.dp)
                ) {
                    if (!isImeVisible) {
                        CtaButton(
                            text = "다음으로",
                            enabled = stepUiState.canGoNext,
                            onClick = onNext,
                            iconResId = R.drawable.ic_arrow_forward
                        )
                    }
                }

            }
        }

        LoadingOverlay(isLoading = stepUiState.isLoading)
    }
}

/* ---------------------------------------------
 * Nickname Input (Pure UI)
 * --------------------------------------------- */

@Composable
fun NicknameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController,
    modifier: Modifier = Modifier,
    maxLength: Int = MAX_LENGTH,
    onDone: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = when {
                        isError -> Color.Red
                        isFocused -> Color.Black
                        else -> Color.LightGray
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart // 🔹 입력 텍스트와 placeholder 중앙 정렬
        ) {

            // ===== Placeholder =====
            if (value.isEmpty()) {
                Text(
                    text = "닉네임을 입력해주세요",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }

            // ===== BasicTextField =====
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    val filtered = if (newValue.length <= maxLength) newValue else newValue.take(maxLength)
                    onValueChange(filtered)
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController.hide()
                        onDone()
                    }
                )
            )

            // ===== 글자 수 표시 =====
            Text(
                text = "${value.length}/$maxLength",
                fontSize = 12.sp,
                color = if (value.length > maxLength) Color.Red else Color.Gray,
                modifier = Modifier.align(Alignment.CenterEnd) // 오른쪽 끝에 정렬
            )
        }

        // ===== 에러 메시지 =====
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp
            )
        }
    }
}






/* ---------------------------------------------
 * Keyboard Helper
 * --------------------------------------------- */

@Composable
public fun rememberKeyboardState(): Triple<Boolean, SoftwareKeyboardController, FocusRequester> {
    val imePadding = WindowInsets.ime.asPaddingValues()
    val isImeVisible = imePadding.calculateBottomPadding().value > 0
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val safeKeyboardController = keyboardController ?: object : SoftwareKeyboardController {
        override fun show() {}
        override fun hide() {}
    }

    return Triple(isImeVisible, safeKeyboardController, focusRequester)
}

/* ---------------------------------------------
 * Preview
 * --------------------------------------------- */

@Preview(showBackground = true)
@Composable
private fun NicknameStepPreview() {
    WalkItTheme {
        NicknameStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(
                    value = "닉네임예시",
                    validationError = null,
                    isDuplicate = false
                )
            ),
            onNicknameChange = {},
            onCheckDuplicate = {},
            onNext = {}
        )
    }
}