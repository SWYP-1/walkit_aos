package swyp.team.walkit.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.White

@Composable
fun HomeHeader(
    profileImageUrl: String?,
    onClickAlarm: () -> Unit,
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

                IconButton(
                    onClick = onClickAlarm,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_alarm),
                        contentDescription = "alarm"
                    )
                }
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImageUrl)
                        .error(R.drawable.ic_default_user) // 프로필 이미지 로드 실패 시 기본 아이콘
                        .build(),
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}


@Composable
@Preview
fun AppHeaderPreview() {
    WalkItTheme {
        HomeHeader(
            "https://images.pexels.com/photos/3861976/pexels-photo-3861976.jpeg?_gl=1*8iaqp3*_ga*ODU3MTU1NTU2LjE3NjYwMzk4MDQ.*_ga_8JE65Q40S6*czE3NjYwMzk4MDQkbzEkZzEkdDE3NjYwMzk4MzEkajMzJGwwJGgw",
            {})
    }
}