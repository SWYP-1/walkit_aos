package swyp.team.walkit.ui.record.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.White

@Composable
fun RecordHeader(
    onClickSearch: () -> Unit = {},
    onClickAlarm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .height(60.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // 로고
            Image(
                painter = painterResource(R.drawable.img_logo),
                contentDescription = "서비스 로고"
            )

            // 액션 아이콘 영역
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
//                IconButton(
//                    onClick = onClickSearch
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_action_search),
//                        contentDescription = "검색",
//                        modifier = Modifier.size(24.dp)
//                    )
//                }

                IconButton(
                    onClick = onClickAlarm
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
}

@Composable
@Preview
fun RecordHeaderPreview() {
    WalkItTheme {
        RecordHeader({}, {})
    }
}