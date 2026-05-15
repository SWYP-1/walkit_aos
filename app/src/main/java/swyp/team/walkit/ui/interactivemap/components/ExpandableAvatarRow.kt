package swyp.team.walkit.ui.interactivemap.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onAvatarClick: (userId: Long) -> Unit = {},
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
        // 제일 좌측 — 상태 토글 버튼 (+ / ×)
        AvatarRowToggle(
            isOpen = expanded,
            onClick = { expanded = !expanded },
        )

        Spacer(Modifier.width(8.dp))

        if (expanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(activities) { activity ->
                    LottieAvatar(
                        activity = activity,
                        lottieJson = lottieJsonMap[activity.userId],
                        showName = true,
                        onClick = { onAvatarClick(activity.userId) },
                    )
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
                        onClick = { onAvatarClick(activity.userId) },
                    )
                }
            }
        }

        // 제일 우측 — 친구 목록 이동 버튼
        Spacer(Modifier.width(8.dp))

        if(expanded){
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color = SemanticColor.buttonPrimaryDefault)
                    .clickable { onNavigateToFriends() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cheven_right),
                    contentDescription = "친구 목록으로 이동",
                    tint = SemanticColor.iconWhite,
                    modifier = Modifier.size(20.dp),
                )
            }
        }else {
            val restCount = activities.size - visibleItems.size;
            if(restCount > 0){
                MoreFriendCountText(
                    count = restCount
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
    showName: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 아바타 + 빨간 점 배지를 겹쳐서 표시
        Box(contentAlignment = Alignment.TopEnd) {
            // 아바타 원형 — clip을 먼저 적용해야 리플이 원형으로 바운드됨
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, color = SemanticColor.textBorderDisabled, shape = CircleShape)
                    .background(SemanticColor.backgroundWhitePrimary)
                    .then(
                        if (onClick != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onClick,
                        ) else Modifier
                    ),
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
                            .scale(1.1f)
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
                )
            }
        }

        // 펼쳐진 상태에서 닉네임 표시 (최대 5글자, 초과 시 … 처리)
        if (showName) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = activity.nickName,
                style = MaterialTheme.walkItTypography.captionM.copy(
                    fontSize = 8.sp,
                    lineHeight = 10.4.sp,
                    textAlign = TextAlign.Center,
                    color = SemanticColor.textBorderSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(30.dp),
            )
        }
    }
}

@Composable
fun MoreFriendCountText(modifier: Modifier = Modifier,count : Int) {
    Text(
        text = "+$count",
        style = MaterialTheme.walkItTypography.bodyXL.copy(
            fontWeight = FontWeight.Normal,
            color = SemanticColor.textBorderSecondary,
        ),
    )
}

@Composable
fun AvatarRowToggle(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "AvatarRowToggle rotation",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isOpen) SemanticColor.backgroundWhiteQuaternary else SemanticColor.buttonPrimaryDefault,
        animationSpec = tween(durationMillis = 250),
        label = "AvatarRowToggle bg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isOpen) SemanticColor.iconGrey else SemanticColor.iconWhite,
        animationSpec = tween(durationMillis = 250),
        label = "AvatarRowToggle tint",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = if (isOpen) "닫기" else "펼치기",
            tint = iconTint,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
        )
    }
}
