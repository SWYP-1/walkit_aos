package team.swyp.sdu.ui.dressroom.component

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography


@Composable
fun ItemCard(
    itemImageUrl: String,
    name: String,
    position: EquipSlot,
    point: Int,
    isMine: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (nameBackGround, textColor) = when (position) {
        EquipSlot.HEAD -> SemanticColor.statePinkTertiary to SemanticColor.statePinkPrimary
        EquipSlot.BODY -> SemanticColor.statePurpleTertiary to SemanticColor.statePurplePrimary
        EquipSlot.FEET -> SemanticColor.stateBlueTertiary to SemanticColor.stateBluePrimary
    }

    // 배경색 우선순위: 착용 > 선택 > 기본
    val cardBackgroundColor = when {
        isSelected -> SemanticColor.backgroundGreenPrimary // 선택 시 초록색 배경
        else -> SemanticColor.backgroundWhitePrimary // 기본 흰색
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight() // ⭐ 컨텐츠 기준 높이
            .background(
                color = cardBackgroundColor, shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp, color = when {
                    isSelected -> SemanticColor.stateGreenPrimary
                    else -> SemanticColor.textBorderSecondaryInverse
                }, shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {

        // MY 뱃지
        if (isMine) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .offset(x = -4.dp, y = -4.dp)
                    .background(
                        color = SemanticColor.textBorderTertiary, shape = CircleShape
                    )
                    .align(Alignment.TopStart), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MY", style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold
                    ), color = SemanticColor.textBorderPrimaryInverse
                )
            }
        }

        // 아이템 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 이미지
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(
                        1.dp, SemanticColor.backgroundWhiteTertiary, CircleShape
                    ), contentAlignment = Alignment.Center
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 이름 배지
            Box(
                modifier = Modifier.background(
                        nameBackGround, RoundedCornerShape(16.dp)
                    ), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name, modifier = Modifier.padding(
                        horizontal = 8.dp, vertical = 4.dp
                    ), style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold
                    ), color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 포인트
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp) // ⭐ 가로 = 세로
                        .background(
                            SemanticColor.stateYellowTertiary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.stateYellowPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$point", style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ), color = SemanticColor.textBorderPrimary
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ItemCard(
                itemImageUrl = "https://picsum.photos/200",
                name = "모자",
                point = 1200,
                isMine = true,
                isSelected = false,
                position = EquipSlot.HEAD,
                onClick = {},
                modifier = Modifier.width(100.dp)
            )
            ItemCard(
                itemImageUrl = "https://picsum.photos/200",
                name = "상의",
                point = 1500,
                isMine = false,
                isSelected = true,
                position = EquipSlot.BODY,
                onClick = {},
                modifier = Modifier.width(100.dp)
            )
            ItemCard(
                itemImageUrl = "https://picsum.photos/200",
                name = "신발",
                point = 800,
                isMine = true,
                isSelected = false,
                position = EquipSlot.FEET,
                onClick = {},
                modifier = Modifier.width(100.dp)
            )
        }
    }
}
