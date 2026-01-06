package swyp.team.walkit.ui.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.Friend
import swyp.team.walkit.domain.model.User
import swyp.team.walkit.ui.record.friendrecord.component.FriendRecordSkeletonRow
import swyp.team.walkit.ui.theme.SemanticColor

/**
 * 기록 화면 상단 섹션 컴포넌트
 */
@Composable
fun RecordTopSection(
    user: User?,
    friends: List<Friend>,
    selectedFriendNickname: String?,
    onMyProfileClick: () -> Unit,
    onFriendSelected: (Friend) -> Unit,
    onNavigateToFriend: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(start = 20.dp, top = 8.dp, bottom = 14.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MyProfileImage(
            user = user,
            onClick = {
                android.util.Log.d("RecordTopSection", "MyProfileImage 클릭됨")
                timber.log.Timber.d("MyProfileImage 클릭됨")
                onMyProfileClick()
            },
            isSelected = selectedFriendNickname == null
        )
        Spacer(Modifier.width(8.dp))
        VerticalDivider(
            thickness = 1.dp,
            color = SemanticColor.backgroundWhiteQuaternary,
            modifier = Modifier.height(32.dp)
        )
        Spacer(Modifier.width(8.dp))
        FriendListRow(
            friends = friends,
            selectedFriendNickname = selectedFriendNickname,
            onFriendSelected = { friend ->
                android.util.Log.d("RecordTopSection", "RecordTopSection에서 친구 선택됨: ${friend.nickname}")
                timber.log.Timber.d("RecordTopSection에서 친구 선택됨: ${friend.nickname}")
                onFriendSelected(friend)
            },
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onNavigateToFriend,
            modifier = Modifier.size(55.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "friend list more",
                tint = SemanticColor.iconGrey
            )
        }
    }
}

/**
 * 기록 화면 상단 섹션 Skeleton 컴포넌트
 * 실제 RecordTopSection과 동일한 구조로 로딩 상태 표시
 */
@Composable
fun RecordTopSectionSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(start = 20.dp, top = 8.dp, bottom = 14.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // MyProfileImage 자리 (Skeleton)
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.LightGray, CircleShape)
        )

        Spacer(Modifier.width(8.dp))

        // VerticalDivider 자리
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(Color.LightGray)
        )

        Spacer(Modifier.width(8.dp))

        // FriendListRow 자리 (실제 Skeleton)
        FriendRecordSkeletonRow(
            modifier = Modifier.weight(1f)
        )

        // IconButton 자리 (Skeleton)
        Box(
            modifier = Modifier
                .size(55.dp)
                .background(Color.LightGray, CircleShape)
        )
    }
}



