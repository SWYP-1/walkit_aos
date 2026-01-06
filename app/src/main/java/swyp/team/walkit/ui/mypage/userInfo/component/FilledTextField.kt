package swyp.team.walkit.ui.mypage.userInfo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.Grey2
import swyp.team.walkit.ui.theme.Grey5
import swyp.team.walkit.ui.theme.walkItTypography


@Composable
fun FilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(Grey2, RoundedCornerShape(8.dp)),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.walkItTypography.bodyM,
                color = Grey5,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = Grey10,
            unfocusedTextColor = Grey10,
            errorBorderColor = Color.Red,
        ),
        shape = RoundedCornerShape(8.dp),
        textStyle = MaterialTheme.walkItTypography.bodyM,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
    )
}
