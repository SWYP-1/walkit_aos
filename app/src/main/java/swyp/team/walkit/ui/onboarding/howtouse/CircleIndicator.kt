package swyp.team.walkit.ui.onboarding.howtouse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.SemanticColor

@Composable
fun CircleIndicator(
    selectedIndex: Int,
    count: Int = 3
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (index == selectedIndex) SemanticColor.buttonPrimaryDefault else SemanticColor.backgroundWhiteQuaternary,
                        shape = CircleShape
                    )
            )
        }
    }
}