package team.swyp.sdu.ui.mypage.userInfo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 생년월일 드롭다운 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val tertiaryText = SemanticColor.textBorderTertiary

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = tertiaryText,
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = SemanticColor.textBorderPrimary,
                    unfocusedTextColor = SemanticColor.textBorderPrimary,
                ),
            shape = RoundedCornerShape(4.dp),
            textStyle = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.Medium,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Grey3, RoundedCornerShape(4.dp))
                .menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // 년도/월/일 목록 생성
            when (placeholder) {
                "년도" -> {
                    // 최근 100년
                    (1924..2024).reversed().forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$year",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                )
                            },
                            onClick = {
                                onValueChange("$year")
                                expanded = false
                            },
                        )
                    }
                }

                "월" -> {
                    (1..12).forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$month",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                )
                            },
                            onClick = {
                                onValueChange("$month")
                                expanded = false
                            },
                        )
                    }
                }

                "일" -> {
                    (1..31).forEach { day ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                )
                            },
                            onClick = {
                                onValueChange("$day")
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
