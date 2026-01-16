package swyp.team.walkit.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import java.time.LocalDate
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.CtaButtonVariant
import swyp.team.walkit.ui.components.PreviousButton
import swyp.team.walkit.ui.components.LoadingOverlay
import swyp.team.walkit.ui.onboarding.component.OnBoardingStepTag
import swyp.team.walkit.ui.theme.Black
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 출생년월일 선택 단계 컴포넌트
 */
@Composable
fun BirthYearStep(
    uiState: OnboardingUiState,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    // 생년월일 업데이트 API 호출 시 로딩 표시
    val isLoading = uiState.isLoading && uiState.currentStep == 1
    val currentYear = uiState.birthYear
    val currentMonth = uiState.birthMonth
    val currentDay = uiState.birthDay

    // 텍스트 필드 값 상태 관리
    var yearText by remember { mutableStateOf(TextFieldValue("")) }
    var monthText by remember { mutableStateOf(TextFieldValue("")) }
    var dayText by remember { mutableStateOf(TextFieldValue("")) }

    // 해당 월의 마지막 날짜 계산
    val daysInMonth = remember(currentYear, currentMonth) {
        try {
            if (currentYear > 0 && currentMonth > 0) {
                LocalDate.of(currentYear, currentMonth, 1).lengthOfMonth()
            } else {
                31
            }
        } catch (t: Throwable) {
            31
        }
    }

    // 유효성 검사 결과
    val isValidDate = remember(currentYear, currentMonth, currentDay) {
        val yearValid = currentYear in 1901..LocalDate.now().year
        val monthValid = currentMonth in 1..12
        val dayValid = try {
            if (currentYear > 0 && currentMonth > 0 && currentDay > 0) {
                LocalDate.of(currentYear, currentMonth, currentDay)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
        yearValid && monthValid && dayValid
    }

    // 모든 필드가 채워졌는지 확인 (실제 값으로 판단)
    val allFieldsFilled = remember(currentYear, currentMonth, currentDay) {
        currentYear > 0 && currentMonth > 0 && currentDay > 0
    }

//    // 일자가 유효 범위를 벗어나면 자동으로 조정
//    LaunchedEffect(currentYear, currentMonth, currentDay, daysInMonth) {
//        if (currentYear > 0 && currentMonth > 0 && currentDay > 0) {
//            val safeDay = currentDay.coerceIn(1, daysInMonth)
//            if (safeDay != currentDay) {
//                onDayChange(safeDay)
//                dayText = TextFieldValue(String.format("%02d", safeDay))
//            }
//        }
//    }

//    // 모든 필드가 채워졌을 때 유효성 검사 실행
//    LaunchedEffect(allFieldsFilled, currentYear, currentMonth, currentDay) {
//        if (allFieldsFilled && !isValidDate) {
//            // 유효하지 않은 날짜인 경우, 일자를 자동 조정
//            if (currentYear > 0 && currentMonth > 0 && currentDay > 0) {
//                try {
//                    LocalDate.of(currentYear, currentMonth, currentDay)
//                } catch (t: Throwable) {
//                    // 유효하지 않은 날짜인 경우, 해당 월의 마지막 날로 조정
//                    val safeDay = daysInMonth.coerceAtMost(31)
//                    onDayChange(safeDay)
//                    dayText = TextFieldValue(String.format("%02d", safeDay))
//                }
//            }
//        }
//    }

    // 년도 입력 처리
    fun handleYearInput(newValue: TextFieldValue) {
        val text = newValue.text.filter { it.isDigit() }
        if (text.length <= 4) {
            yearText = newValue.copy(text = text)
            if (text.length == 4) {
                val year = text.toIntOrNull() ?: 0
                if (year in 1901..LocalDate.now().year) {
                    onYearChange(year)
                } else {
                    // 유효하지 않은 년도인 경우 0으로 설정
                    onYearChange(0)
                }
            } else if (text.isEmpty()) {
                onYearChange(0)
            }
        }
    }

    // 월 입력 처리
    fun handleMonthInput(newValue: TextFieldValue) {
        val text = newValue.text.filter { it.isDigit() }
        if (text.length <= 2) {
            monthText = newValue.copy(text = text)
            if (text.length == 2) {
                val month = text.toIntOrNull() ?: 0
                if (month in 1..12) {
                    onMonthChange(month)
                } else {
                    // 유효하지 않은 월인 경우 0으로 설정
                    onMonthChange(0)
                }
            } else if (text.isEmpty()) {
                onMonthChange(0)
            }
        }
    }

    // 일 입력 처리
    fun handleDayInput(newValue: TextFieldValue) {
        val text = newValue.text.filter { it.isDigit() }
        if (text.length <= 2) {
            dayText = newValue.copy(text = text)
            if (text.length == 2) {
                val day = text.toIntOrNull() ?: 0
                if (day > 0) {
                    // 유효성 검사는 LaunchedEffect에서 처리
                    onDayChange(day)
                }
            } else if (text.isEmpty()) {
                onDayChange(0)
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(SemanticColor.backgroundWhiteSecondary)) {
        Column(
            Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp)) {

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnBoardingStepTag(text = "준비 단계")
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${uiState.nicknameState.value}님,\n" + "생년월일을 선택해주세요",
                        style = MaterialTheme.walkItTypography.headingM.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Black,
                        textAlign = TextAlign.Center
                    )
                }



                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "생년월일", style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ), color = Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "생년월일 8자리를 입력해주세요.",
                    style = MaterialTheme.walkItTypography.captionM,
                    color = SemanticColor.textBorderSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 날짜 입력 필드 (년, 월, 일)
                // 각 필드별 유효성 검사: 입력된 값이 있고 유효하지 않을 때만 에러 표시
                val yearError = yearText.text.length == 4 && !(currentYear in 1901..LocalDate.now().year)
                val monthError = monthText.text.length == 2 && !(currentMonth in 1..12)
                val dayError = dayText.text.length == 2 && currentDay > 0 && !isValidDate
                val isError = yearError || monthError || dayError

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 년도 입력 필드
                    DateNumberInputField(
                        value = yearText,
                        placeholder = "YYYY",
                        modifier = Modifier.weight(1f),
                        onValueChange = ::handleYearInput,
                        maxLength = 4,
                        isError = isError
                    )

                    // 월 입력 필드
                    DateNumberInputField(
                        value = monthText,
                        placeholder = "MM",
                        modifier = Modifier.weight(1f),
                        onValueChange = ::handleMonthInput,
                        maxLength = 2,
                        isError = isError
                    )

                    // 일 입력 필드
                    DateNumberInputField(
                        value = dayText,
                        placeholder = "DD",
                        modifier = Modifier.weight(1f),
                        onValueChange = ::handleDayInput,
                        maxLength = 2,
                        isError = isError
                    )
                }

                // 유효성 검사 에러 메시지 표시
                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "올바른 날짜를 입력해주세요.",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.stateRedPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviousButton(
                        onClick = onPrev
                    )

                    CtaButton(
                        text = "다음으로",
                        onClick = onNext,
                        enabled = allFieldsFilled && isValidDate,  // 입력 완료되고 유효할 때만 활성화
                        modifier = Modifier.weight(1f),
                        iconResId = R.drawable.ic_arrow_forward
                    )
                }
            }

            // API 요청 시 로딩 오버레이 표시
            LoadingOverlay(isLoading = isLoading)
        }
    }
}

/**
 * 날짜 입력용 숫자 텍스트 필드 컴포넌트
 */
@Composable
private fun DateNumberInputField(
    value: TextFieldValue,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    isError: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(
                width = 1.dp,
                color = if (isError) SemanticColor.stateRedPrimary
                else SemanticColor.textBorderPrimary,
                shape = RoundedCornerShape(8.dp)
            )
            .background(SemanticColor.backgroundWhitePrimary)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.walkItTypography.bodyM.copy(
                color = if (value.text.isEmpty()) SemanticColor.textBorderSecondary else SemanticColor.textBorderPrimary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            decorationBox = { innerTextField ->
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.walkItTypography.bodyM,
                        color = SemanticColor.textBorderSecondary
                    )
                }
                innerTextField()
            }
        )
    }
}

@Preview(showBackground = true, name = "기본 상태 - 날짜 미선택")
@Composable
private fun BirthYearStepEmptyPreview() {
    WalkItTheme {
        BirthYearStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(filteredValue = "홍길동"),
                birthYear = 0,
                birthMonth = 0,
                birthDay = 0,
            ),
            onYearChange = {},
            onMonthChange = {},
            onDayChange = {},
            onNext = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "날짜 선택됨 - 1998년 5월 15일")
@Composable
private fun BirthYearStepPreview() {
    WalkItTheme {
        BirthYearStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState(filteredValue = "홍길동"),
                birthYear = 1998,
                birthMonth = 5,
                birthDay = 15,
            ),
            onYearChange = {},
            onMonthChange = {},
            onDayChange = {},
            onNext = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "날짜 선택됨 - 2010년 12월 25일")
@Composable
private fun BirthYearStepYoungPreview() {
    WalkItTheme {
        BirthYearStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState("김철수"),
                birthYear = 2010,
                birthMonth = 12,
                birthDay = 25,
            ),
            onYearChange = {},
            onMonthChange = {},
            onDayChange = {},
            onNext = {},
            onPrev = {},
        )
    }
}

@Preview(showBackground = true, name = "날짜 선택됨 - 2000년 2월 29일 (윤년)")
@Composable
private fun BirthYearStepLeapYearPreview() {
    WalkItTheme {
        BirthYearStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState("이영희"),
                birthYear = 2000,
                birthMonth = 2,
                birthDay = 29,
            ),
            onYearChange = {},
            onMonthChange = {},
            onDayChange = {},
            onNext = {},
            onPrev = {},
        )
    }
}

