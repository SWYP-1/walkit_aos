package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import team.swyp.sdu.domain.model.Friend

/**
 * 친구 목록 가로 스크롤 컴포넌트
 */
@Composable
fun FriendListRow(
    friends: List<Friend>,
    selectedFriendNickname: String?,
    onFriendSelected: (Friend) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), // 아이템 간격 추가
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(friends) { friend ->
            FriendAvatarItem(
                friend = friend,
                isSelected = friend.nickname == selectedFriendNickname,
                onClick = {
                    android.util.Log.d("FriendListRow", "FriendListRow에서 친구 선택됨: ${friend.nickname}")
                    timber.log.Timber.d("FriendListRow에서 친구 선택됨: ${friend.nickname}")
                    onFriendSelected(friend)
                },
            )
        }
    }
}



