package team.swyp.sdu.ui.record.dailyrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.R
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.components.RouteThumbnail
import team.swyp.sdu.ui.theme.Grey4
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.DateUtils
import java.io.File

/**
 * 세션 썸네일 컴포넌트 (선택된 세션 하나만 표시)
 *
 * @param session 선택된 세션 (nullable)
 * @param onExternalClick 세션 선택 콜백
 * @param modifier Modifier
 */
@Composable
fun SessionThumbnailList(
    session: WalkingSession?,
    onExternalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "2025년 12월 15일 ",
                // body L/medium
                style = MaterialTheme.walkItTypography.bodyL.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary
            )

            IconButton(
                onClick = onExternalClick,
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_external),
                    contentDescription = "external",
                    tint = SemanticColor.iconGrey
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        if (session != null) {
            SessionThumbnailItem(
                session = session,
                isSelected = true,
                onClick = { onExternalClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}

/**
 * 개별 세션 썸네일 아이템
 *
 * @param session 산책 세션
 * @param isSelected 선택 여부
 * @param onClick 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun SessionThumbnailItem(
    session: WalkingSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(Color.White)
                        .padding(2.dp)
                } else {
                    Modifier
                }
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 이미지 URI 가져오기 (localImagePath -> serverImageUrl 순서)
            val imageUri = session.getImageUri()

            if (imageUri != null) {
                // 이미지가 있으면 이미지 표시
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(
                            if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                                // 서버 URL인 경우
                                imageUri
                            } else {
                                // 로컬 파일 경로인 경우
                                File(imageUri)
                            }
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = "산책 기록 썸네일",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                // 이미지가 없으면 경로 썸네일 표시
                RouteThumbnail(
                    locations = session.locations,
                    modifier = Modifier.fillMaxSize(),
                    height = 200.dp,
                )
            }
        }

        val startTimeWithSeconds = DateUtils.formatToTimeHHMMSS(session.startTime)
        val enTimeWithSeconds = DateUtils.formatToTimeHHMMSS(session.endTime)

//        Text(
//            text = "$startTimeWithSeconds ~ $enTimeWithSeconds",
//            // body S/semibold
//            style = MaterialTheme.walkItTypography.bodyS.copy(
//                fontWeight = FontWeight.SemiBold
//            ),
//            color = SemanticColor.textBorderPrimaryInverse,
//            modifier = Modifier
//                .align(alignment = Alignment.BottomEnd)
//                .padding(16.dp)
//        )
    }
}