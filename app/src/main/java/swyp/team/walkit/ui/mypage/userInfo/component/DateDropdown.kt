package swyp.team.walkit.ui.mypage.userInfo.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import swyp.team.walkit.ui.record.components.DropMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.Grey3
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

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
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)


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
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = "drop icon",
                    modifier = Modifier.rotate(rotation)
                )
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
            modifier = Modifier.background(SemanticColor.backgroundWhitePrimary) // 드롭다운 메뉴 배경색 변경
        ) {
            // 년도/월/일 목록 생성
            when (placeholder) {
                "년도" -> {
                // 최근 100년
                (1924..2024).reversed().forEach { year ->
                    DropMenuItem(
                        text = "$year",
                        onClick = {
                            onValueChange("$year")
                            expanded = false
                        }
                    )
                }
                }

                "월" -> {
                    (1..12).forEach { month ->
                        DropMenuItem(
                            text = "$month",
                            onClick = {
                                onValueChange("$month")
                                expanded = false
                            },
                        )
                    }
                }

                "일" -> {
                    (1..31).forEach { day ->
                        DropMenuItem(
                            text = "$day",
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

// Preview functions
@Preview(showBackground = true, name = "년도 드롭다운")
@Composable
private fun YearDropdownPreview() {
    WalkItTheme {
        DateDropdown(
            value = "1990",
            onValueChange = {},
            placeholder = "년도",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "년도 드롭다운 - 빈 값")
@Composable
private fun YearDropdownEmptyPreview() {
    WalkItTheme {
        DateDropdown(
            value = "",
            onValueChange = {},
            placeholder = "년도",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "월 드롭다운")
@Composable
private fun MonthDropdownPreview() {
    WalkItTheme {
        DateDropdown(
            value = "5",
            onValueChange = {},
            placeholder = "월",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "월 드롭다운 - 빈 값")
@Composable
private fun MonthDropdownEmptyPreview() {
    WalkItTheme {
        DateDropdown(
            value = "",
            onValueChange = {},
            placeholder = "월",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "일 드롭다운")
@Composable
private fun DayDropdownPreview() {
    WalkItTheme {
        DateDropdown(
            value = "15",
            onValueChange = {},
            placeholder = "일",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "일 드롭다운 - 빈 값")
@Composable
private fun DayDropdownEmptyPreview() {
    WalkItTheme {
        DateDropdown(
            value = "",
            onValueChange = {},
            placeholder = "일",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
