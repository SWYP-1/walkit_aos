package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey5
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 공통 양식 필드 컴포넌트
 *
 * 텍스트 입력을 위한 공통 양식 컴포넌트
 *
 * @param label 필드 라벨 (선택사항)
 * @param value 입력 값
 * @param onValueChange 값 변경 콜백
 * @param placeholder 플레이스홀더 텍스트
 * @param helperText 도움말 텍스트 (선택사항)
 * @param errorText 에러 메시지 (선택사항)
 * @param isError 에러 상태 여부
 * @param keyboardType 키보드 타입
 * @param visualTransformation 텍스트 변환 (비밀번호 등)
 * @param modifier Modifier
 */
@Composable
fun FormField(
    label: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    helperText: String? = null,
    errorText: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 라벨
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.walkItTypography.bodyM,
                color = Grey10,
            )
        }

        // 입력 필드
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.walkItTypography.bodyL.copy(
                color = if (isError) Color(0xFFE53E3E) else Grey10,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isError -> Color(0xFFE53E3E)
                                else -> Grey5
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.walkItTypography.bodyL,
                            color = Grey5,
                        )
                    }
                    innerTextField()
                }
            },
        )

        // 도움말 또는 에러 메시지
        val messageText = errorText ?: helperText
        val messageColor = if (isError) Color(0xFFE53E3E) else Grey5

        if (messageText != null) {
            Text(
                text = messageText,
                style = MaterialTheme.walkItTypography.captionM,
                color = messageColor,
            )
        }
    }
}

/**
 * 숫자 전용 양식 필드 컴포넌트
 */
@Composable
fun NumberFormField(
    label: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    helperText: String? = null,
    errorText: String? = null,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    FormField(
        label = label,
        value = value,
        onValueChange = { newValue ->
            // 숫자만 허용
            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                onValueChange(newValue)
            }
        },
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        isError = isError,
        keyboardType = KeyboardType.Number,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun FormFieldPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 기본 텍스트 필드
            FormField(
                label = "이름",
                value = "",
                onValueChange = {},
                placeholder = "이름을 입력하세요",
                helperText = "실명을 입력해주세요",
            )

            // 에러 상태
            FormField(
                label = "이메일",
                value = "invalid-email",
                onValueChange = {},
                placeholder = "이메일을 입력하세요",
                errorText = "올바른 이메일 형식이 아닙니다",
                isError = true,
            )

            // 숫자 필드
            NumberFormField(
                label = "나이",
                value = "25",
                onValueChange = {},
                placeholder = "나이를 입력하세요",
                helperText = "만 14세 이상만 가입 가능합니다",
            )
        }
    }
}








