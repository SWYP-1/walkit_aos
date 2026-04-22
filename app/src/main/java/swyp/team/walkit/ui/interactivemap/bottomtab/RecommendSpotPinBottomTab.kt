package swyp.team.walkit.ui.interactivemap.bottomtab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.NearbySpot
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 추천 장소 상세 콘텐츠
 *
 * @param spot    표시할 장소 도메인 모델
 * @param onClose X 버튼 클릭 시 콜백 (SpotList로 복귀)
 */
@Composable
fun RecommendSpotPinBottomTab(
    spot: NearbySpot,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 36.dp),
    ) {
        // 드래그 핸들 여백
        Spacer(modifier = Modifier.height(8.dp))

        // 장소명
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = spot.placeName,
                style = MaterialTheme.walkItTypography.bodyXL.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColor.textBorderPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(32.dp) // 터치 영역 확보
                    .background(
                        color = Color(0xFFD9D9D9),
                        shape = CircleShape
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClose,
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_clear),
                    tint = SemanticColor.iconGrey,
                    contentDescription = "close",
                    modifier = Modifier.size(20.dp),
                )
            }


        }

        Spacer(modifier = Modifier.height(1.5.dp))

        Text(
            text = spot.distance + " m",
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Medium,
                color = SemanticColor.textBorderSecondary
            ),
        )
        Spacer(modifier = Modifier.height(18.dp))

        // key: URL이 바뀌면 컴포저블 완전 재생성 → 이전 이미지 상태 잔상 방지
        key(spot.thumbnailUrl) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(spot.thumbnailUrl)
                .crossfade(false)
                .build(),
            contentDescription = "장소 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Error,
                is AsyncImagePainter.State.Empty -> {
                    // 이미지 로드 실패 또는 URL 없음 → 회색 배경 + 중앙 아이콘
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(SemanticColor.backgroundWhiteSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_face_smile),
                            contentDescription = null,
                            modifier = Modifier
                                .width(92.dp)
                                .height(85.dp),
                        )
                    }
                }
                else -> SubcomposeAsyncImageContent()
            }
        }
        } // key
    }
}
