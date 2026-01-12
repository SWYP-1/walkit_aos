package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.Grey2
import swyp.team.walkit.ui.theme.SemanticColor


@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SemanticColor.backgroundWhiteSecondary, // color/background/white-secondary
                shape = RoundedCornerShape(8.dp), // radius/8px (Figma 디자인에 맞춤)
            ),
    ) {
        content()
    }
}

