package swyp.team.walkit.ui.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import swyp.team.walkit.ui.record.components.customShadow
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun WalkingFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(100.dp)
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 10.dp,
                    color = Color(0x26000000),
                    offset = DpOffset(0.dp, 4.dp)   // 아래 그림자
                )
            ),
        shape = CircleShape,
        containerColor = SemanticColor.stateGreenTertiary, // #F3FFF8
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 3.dp,
                    color = SemanticColor.stateGreenPrimary, // #52CE4B
                    shape = CircleShape,
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_action_walk),
                contentDescription = "산책하기 아이콘",
                modifier = Modifier.size(46.dp),
            )
            
            Text(
                text = "산책하기",
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = SemanticColor.stateGreenPrimary, // #52CE4B
                textAlign = TextAlign.Center,
            )
        }
    }
}

