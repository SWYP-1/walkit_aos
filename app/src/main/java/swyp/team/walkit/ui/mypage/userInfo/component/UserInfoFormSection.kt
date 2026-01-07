package swyp.team.walkit.ui.mypage.userInfo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swyp.team.walkit.ui.onboarding.rememberKeyboardState
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.Grey2
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 사용자 정보 입력 폼 섹션 컴포넌트
 * 이름, 닉네임, 생년월일 입력 필드들을 포함
 */
@Composable
fun UserInfoFormSection(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    birthYear: String,
    onBirthYearChange: (String) -> Unit,
    birthMonth: String,
    onBirthMonthChange: (String) -> Unit,
    birthDay: String,
    onBirthDayChange: (String) -> Unit,
    isNicknameDuplicate: Boolean?,
    nicknameValidationError: String?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val redPrimary = SemanticColor.stateRedPrimary
    val tertiaryText = SemanticColor.textBorderTertiary

    val (isImeVisible, keyboardController, focusRequester) = rememberKeyboardState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {

        // 닉네임 입력 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "닉네임",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Grey10,
                )
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = redPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))

            NicknameInputField(
                value = nickname,
                onValueChange = onNicknameChange,
                isError = isNicknameDuplicate == true || nicknameValidationError != null,
                errorMessage = when {
                    isNicknameDuplicate == true -> "중복된 닉네임입니다."
                    nicknameValidationError != null -> nicknameValidationError
                    else -> null
                },
                isLoading = isLoading,
                focusRequester = remember { FocusRequester() },
                keyboardController = keyboardController
            )
        }

        // 생년월일 선택 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "생년월일",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Grey10,
                )
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = redPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 년도 선택
                DateDropdown(
                    value = birthYear,
                    onValueChange = onBirthYearChange,
                    placeholder = "년도",
                    modifier = Modifier.weight(1f),
                )

                // 월 선택
                DateDropdown(
                    value = birthMonth,
                    onValueChange = onBirthMonthChange,
                    placeholder = "월",
                    modifier = Modifier.weight(1f),
                )

                // 일 선택
                DateDropdown(
                    value = birthDay,
                    onValueChange = onBirthDayChange,
                    placeholder = "일",
                    modifier = Modifier.weight(1f),
                )
            }
        }


    }
}

/* ---------------------------------------------
 * Nickname Input (Pure UI)
 * --------------------------------------------- */

private const val MAX_LENGTH = 20

@Composable
fun NicknameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    isLoading: Boolean = false,
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
            // 로딩 중에는 placeholder 표시하지 않음 (깜빡임 방지)
            if (value.isEmpty() && !isLoading) {
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
