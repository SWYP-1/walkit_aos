package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.ui.theme.SemanticColor

/**
 * 친구 아바타 아이템
 */
@Composable
fun FriendAvatarItem(
    friend: Friend,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(46.dp)
            .clickable(
                indication = null, // ripple 제거
                interactionSource = remember { MutableInteractionSource() }
            ) {
                android.util.Log.d("FriendAvatarItem", "친구 클릭됨: ${friend.nickname}")
                timber.log.Timber.d("친구 클릭됨: ${friend.nickname}")
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) SemanticColor.textBorderGreenSecondary else androidx.compose.ui.graphics.Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (friend.avatarUrl != null && friend.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.nickname,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_default_user),
                    error = painterResource(R.drawable.ic_default_user),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_default_user),
                    contentDescription = friend.nickname,
                    modifier = Modifier.size(46.dp),
                )
            }
        }
        // Text(
        //     text = friend.nickname,
        //     style = MaterialTheme.typography.bodySmall,
        //     maxLines = 1,
        //     overflow = TextOverflow.Ellipsis,
        //     color = if (isSelected) {
        //         SemanticColor.textBorderGreenSecondary
        //     } else {
        //         SemanticColor.iconGrey
        //     },
        // )
    }
}



