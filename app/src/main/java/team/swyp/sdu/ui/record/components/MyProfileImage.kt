package team.swyp.sdu.ui.record.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.ui.theme.SemanticColor

/**
 * 내 프로필 이미지 컴포넌트
 */
@Composable
fun MyProfileImage(
    user: User?,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(46.dp)
            .clickable(
                indication = null, // ripple 제거
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
            AsyncImage(
                model = user?.imageName,
                contentDescription = "my profile image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                placeholder = painterResource(R.drawable.ic_default_user),
                error = painterResource(R.drawable.ic_default_user),
            )
        }
    }
}



