package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.components.CustomSwitch
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.createWalkItTypography
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun ItemHeader(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "아이템 목록",
            // body XL/semibold
            style = MaterialTheme.walkItTypography.bodyXL,
            color = SemanticColor.textBorderPrimary,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "보유한 아이템만 보기",
                // body S/regular
                style = MaterialTheme.walkItTypography.bodyS
            )
            Spacer(Modifier.width(8.dp))
            CustomSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ItemHeaderPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        ItemHeader(checked = true, onCheckedChange = {})
    }
}