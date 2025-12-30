package team.swyp.sdu.ui.mypage.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 마이 페이지 헤더 컴포넌트
 *
 * 사용자 닉네임과 등급 배지를 표시합니다.
 */
@Composable
fun MyPageUserInfo(
    nickname: String,
    profileImageUrl: String? = null,
    grade: Grade?,
    modifier: Modifier = Modifier,
) {

    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(modifier = modifier) {
        Text(
            text = nickname,
                style = MaterialTheme.walkItTypography.headingM,
                color = Grey10
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "님",
                style = MaterialTheme.walkItTypography.headingM,
                color = Grey7
            )
            if (grade != null) {
                Spacer(Modifier.width(8.dp))
                GradeBadge(grade = grade)
            }
        }
        Spacer(Modifier.height(32.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profileImageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_default_user)
                    .build(),
                contentDescription = "프로필 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape) // 원형으로 자르기
            )
        }


    }
}


@Preview(showBackground = true)
@Composable
fun MyPageUserInfoPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        MyPageUserInfo(nickname = "테스트사용자", profileImageUrl = "https://example.com/image.jpg", grade = Grade.TREE)
    }
}



