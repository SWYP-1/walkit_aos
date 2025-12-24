package team.swyp.sdu.ui.record.friendrecord.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.ui.record.friendrecord.LikeUiState
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme

@Composable
fun LikeButton(
    state: LikeUiState,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val likeDrawable = if (state.count == 0) R.drawable.ic_action_plus else R.drawable.ic_heart
    Row(
        modifier = modifier
            .background(SemanticColor.stateYellowTertiary, shape = RoundedCornerShape(size = 16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)

            .clickable { onToggleLike() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(likeDrawable),
            contentDescription = "Like",
            tint = if (state.isLiked)
                SemanticColor.stateRedPrimary
            else
                SemanticColor.iconGrey,
            modifier = Modifier.size(16.dp)
        )

        if (state.count > 0) {
            Text(
                text = state.count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.isLiked)
                    SemanticColor.stateRedPrimary
                else
                    SemanticColor.textBorderSecondary
            )
        }

    }
}

@Preview(showBackground = true, name = "좋아요 안 누름 (0개)")
@Composable
private fun LikeButtonNotLikedZeroPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LikeButton(
                state = LikeUiState(count = 0, isLiked = false),
                onToggleLike = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "좋아요 안 누름 (5개)")
@Composable
private fun LikeButtonNotLikedPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LikeButton(
                state = LikeUiState(count = 5, isLiked = false),
                onToggleLike = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "좋아요 누름")
@Composable
private fun LikeButtonLikedPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LikeButton(
                state = LikeUiState(count = 12, isLiked = true),
                onToggleLike = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "모든 상태")
@Composable
private fun LikeButtonAllStatesPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LikeButton(
                state = LikeUiState(count = 0, isLiked = false),
                onToggleLike = {},
            )
            LikeButton(
                state = LikeUiState(count = 5, isLiked = false),
                onToggleLike = {},
            )
            LikeButton(
                state = LikeUiState(count = 12, isLiked = true),
                onToggleLike = {},
            )
            LikeButton(
                state = LikeUiState(count = 123, isLiked = true),
                onToggleLike = {},
            )
        }
    }
}
