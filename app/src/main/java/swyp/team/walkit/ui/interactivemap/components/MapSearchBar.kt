package swyp.team.walkit.ui.interactivemap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun MapSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SemanticColor.backgroundWhitePrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_map_refresh),
            contentDescription = "refresh map",
            tint = Color(0xFF1C1B1F)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "현 지도에서 검색", style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Medium,
                color = SemanticColor.textBorderPrimary
            )
        )

    }
}