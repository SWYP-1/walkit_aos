package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun ItemCard(
    itemImageUrl: String,
    name: String,
    price: Int,
    isMine: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .background(
                color = SemanticColor.backgroundWhitePrimary, shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) SemanticColor.stateGreenPrimary else SemanticColor.textBorderSecondaryInverse,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // MY 뱃지
        if (isMine) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
                    .background(
                        color = SemanticColor.textBorderTertiary,
                        shape = CircleShape
                    )
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MY",
                    // caption M/semibold
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimaryInverse,
                )
            }
//            QuarterCircleWithText(text = "MY", color = Color.Black)

        }

        // 아이템 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(itemImageUrl)
                    .crossfade(true).build(),
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                // caption M/semibold
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFFF6B9D),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "$price P",
                    // body S/medium
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ), color = SemanticColor.textBorderPrimary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "P",
                    // body S/medium
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ), color = SemanticColor.stateYellowPrimary
                )
            }


        }
    }
}

@Composable
fun QuarterCircleWithText(
    text: String, color: Color, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .offset(x = -40.dp, y = -40.dp)
                .size(60.dp)
        ) {
            drawArc(
                color = color, startAngle = 0f, // 좌상단 기준 예시
                sweepAngle = 90f, useCenter = true
            )
        }

        Text(
            text = text, color = Color.Blue, fontWeight = FontWeight.Bold
        )
    }
}


@Preview(
    showBackground = true, backgroundColor = 0xFFF5F5F5
)
@Composable
fun ItemCardPreview() {
    WalkItTheme {
        ItemCard(
            itemImageUrl = "https://picsum.photos/200", // 임시 이미지
            name = "모자",
            price = 1200,
            isMine = true,
            isSelected = true,
            onClick = {},
            modifier = Modifier.width(140.dp) // Grid에서 쓰일 카드 크기 가정
        )
    }
}
