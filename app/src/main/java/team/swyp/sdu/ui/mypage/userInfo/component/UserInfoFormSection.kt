package team.swyp.sdu.ui.mypage.userInfo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey2
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 사용자 정보 입력 폼 섹션 컴포넌트
 * 이름, 닉네임, 생년월일 입력 필드들을 포함
 */
@Composable
fun UserInfoFormSection(
    name: String,
    onNameChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    birthYear: String,
    onBirthYearChange: (String) -> Unit,
    birthMonth: String,
    onBirthMonthChange: (String) -> Unit,
    birthDay: String,
    onBirthDayChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val redPrimary = SemanticColor.stateRedPrimary
    val tertiaryText = SemanticColor.textBorderTertiary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 이름 입력 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = redPrimary,
                )
                Text(
                    text = "이름",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Grey10,
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Grey2, RoundedCornerShape(8.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Grey10,
                    unfocusedTextColor = Grey10,
                    disabledTextColor = tertiaryText,
                    disabledBorderColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Bold,
                ),
                singleLine = true,
                enabled = true,
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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

        // 닉네임 입력 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

            FilledTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                placeholder = "닉네임을 입력해주세요.",
            )
        }
    }
}
