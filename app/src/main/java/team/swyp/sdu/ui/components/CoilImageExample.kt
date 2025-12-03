package team.swyp.sdu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * Coil을 사용한 이미지 로딩 예제
 *
 * Coil의 장점:
 * - Kotlin Coroutines 기반으로 비동기 이미지 로딩
 * - 메모리 캐싱과 디스크 캐싱 자동 처리
 * - Compose와 완벽한 통합
 * - GIF, SVG, WebP 등 다양한 포맷 지원
 * - 플레이스홀더, 에러 이미지 지원
 */
@Composable
fun CoilImageExample(
    imageUrl: String = "https://picsum.photos/300/300",
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.size(200.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = "예제 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 원형 프로필 이미지 예제
 */
@Composable
fun ProfileImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(100.dp)
                .clip(CircleShape),
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(imageUrl ?: "https://via.placeholder.com/100")
                    .crossfade(true)
                    .build(),
            contentDescription = "프로필 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = rememberAsyncImagePainter("https://via.placeholder.com/100"),
            error = rememberAsyncImagePainter("https://via.placeholder.com/100"),
        )
    }
}
