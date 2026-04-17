package swyp.team.walkit.ui.interactivemap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.FollowerRecentActivity
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun ExpandableAvatarRow(
    activities: List<FollowerRecentActivity>,
    lottieJsonMap: Map<Long, String?>,
    modifier: Modifier = Modifier,
    onNavigateToFriends: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    val paddingModifier = if (expanded) {
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    } else {
        Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
    }

    val visibleItems = activities.take(3)

    Row(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(SemanticColor.backgroundGreenPrimary)
            .then(paddingModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                item {
                    CloseButton(onClick = { expanded = false })
                }

                items(activities) { activity ->
                    LottieAvatar(
                        activity = activity,
                        lottieJson = lottieJsonMap[activity.userId],
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .clickable { onNavigateToFriends() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "더보기",
                            style = MaterialTheme.walkItTypography.captionM.copy(
                                fontWeight = FontWeight.Normal,
                                color = SemanticColor.textBorderSecondary,
                            ),
                        )
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-12).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                visibleItems.forEach { activity ->
                    LottieAvatar(
                        activity = activity,
                        lottieJson = lottieJsonMap[activity.userId],
                    )
                }
            }

            if (activities.size > visibleItems.size) {
                Spacer(Modifier.width(4.dp))
                MoreChip(
                    count = activities.size - visibleItems.size,
                    onClick = { expanded = true },
                )
            }
        }
    }
}

/**
 * 팔로워 아바타 — Lottie 캐릭터 상반신 표시 / JSON 없으면 닉네임 이니셜 fallback
 *
 * 어제 산책한 팔로워는 초록 테두리로 강조한다.
 */
@Composable
fun LottieAvatar(
    activity: FollowerRecentActivity,
    lottieJson: String?,
) {
    // 아바타 + 빨간 점 배지를 겹쳐서 표시
    Box(contentAlignment = Alignment.TopEnd) {
        // 아바타 원형
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, color = SemanticColor.textBorderDisabled, shape = CircleShape)
                .clip(CircleShape)
                .background(SemanticColor.backgroundWhitePrimary),
            contentAlignment = Alignment.Center,
        ) {
            if (lottieJson != null) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.JsonString(lottieJson),
                )
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier
                        .fillMaxSize()
                        // 상반신(머리+몸통)만 원형 안에 보이도록 확대 + 아래 오프셋
                        // offset 양수 → 콘텐츠가 아래로 이동 → 원의 클립이 상단(머리)을 보여줌
                        .scale(1.2f)
                        .offset(y = 4.dp),
                )
            } else {
                // Lottie 생성 실패 시 닉네임 이니셜 표시
                Text(
                    text = activity.nickName.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // 어제 산책 시 빨간 점 배지 표시
        if (activity.walkedYesterday) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .border(1.5.dp, Color.White, CircleShape),
            )
        }
    }
}

@Composable
fun MoreChip(
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(Color.LightGray)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.walkItTypography.bodyXL.copy(
                fontWeight = FontWeight.Normal,
                color = SemanticColor.textBorderSecondary,
            ),
        )
        Icon(
            painter = painterResource(R.drawable.ic_cheven_right),
            tint = SemanticColor.iconGrey,
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-4).dp)
                .size(24.dp),
        )
    }
}

@Composable
fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Gray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "X",
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}
