package swyp.team.walkit.ui.record.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.White

@Composable
fun RecordHeader(
    onClickAlarm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(White),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Image(
            painter = painterResource(R.drawable.img_logo),
            contentDescription = "서비스 로고",
            modifier = Modifier.padding(start = 16.dp)
        )
        val interactionSource = remember { MutableInteractionSource() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp), // ⭐ 아이콘 기준 여백
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClickAlarm
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_alarm),
                    contentDescription = "알람",
                    modifier = Modifier.size(24.dp)
                )
            }
        }


    }
}


@Composable
@Preview
fun RecordHeaderPreview() {
    WalkItTheme {
        RecordHeader({})
    }
}