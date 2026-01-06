package swyp.team.walkit.ui.record.friendrecord.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.WalkItTheme

/**
 * 친구 목록 Skeleton UI
 * 실제 FriendListRow의 친구 목록 레이아웃과 1:1로 동일한 구조
 */
@Composable
fun FriendRecordSkeletonRow(
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp), // 실제 UI와 동일
        horizontalArrangement = Arrangement.spacedBy(8.dp), // 실제 UI와 동일한 간격
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(4) { // 고정 4개 아이템
            FriendSkeletonItem()
        }
    }
}

/**
 * 개별 친구 아이템 Skeleton
 * FriendAvatarItem 구조와 동일: 상단 원형 프로필 이미지만 (닉네임 표시되지 않음)
 */
@Composable
private fun FriendSkeletonItem(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(46.dp), // FriendAvatarItem과 동일한 너비
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        // 상단: 46dp 원형 프로필 이미지 placeholder
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .shimmerEffect()
        )

        // 실제 UI에서는 닉네임이 표시되지 않으므로 Skeleton에서도 제거
        // (FriendAvatarItem.kt에서 닉네임 부분이 주석 처리되어 있음)
        // 따라서 높이는 46dp (이미지) + 8dp (간격) = 54dp 정도가 됨
    }
}

/**
 * Shimmer 효과를 위한 Modifier 확장 함수
 * 각 아이템 단위로 shimmer 효과 적용
 */
private fun Modifier.shimmerEffect(): Modifier = composed {
    var shimmerTranslateAnim by remember { mutableStateOf(0f) }

    val shimmerAnim = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by shimmerAnim.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    LaunchedEffect(shimmerTranslate) {
        shimmerTranslateAnim = shimmerTranslate
    }

    this.background(
        color = Color.LightGray.copy(alpha = 0.6f + shimmerTranslateAnim * 0.4f)
    )
}

@Preview(showBackground = true)
@Composable
private fun FriendRecordSkeletonRowPreview() {
    WalkItTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 실제 FriendListRow처럼 배경과 패딩 적용
            FriendRecordSkeletonRow()
        }
    }
}
