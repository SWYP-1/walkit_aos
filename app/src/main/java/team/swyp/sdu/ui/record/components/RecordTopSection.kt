package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.ui.theme.SemanticColor

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
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MyProfileImage(
            user = user,
            onClick = onMyProfileClick,
            isSelected = selectedFriendNickname == null
        )

        FriendListRow(
            friends = friends,
            selectedFriendNickname = selectedFriendNickname,
            onFriendSelected = onFriendSelected,
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


