package swyp.team.walkit.ui.record.friendrecord.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.record.components.DropMenuItem
import swyp.team.walkit.ui.theme.SemanticColor

@Composable
fun FriendRecordMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onBlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
            .width(160.dp)
            .background(SemanticColor.backgroundWhitePrimary),// 드롭다운 메뉴 배경색 변경,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SemanticColor.backgroundWhitePrimary),
        ) {

            // 삭제하기 메뉴 아이템
            DropMenuItem(
                text = "차단하기",
                iconResId = R.drawable.ic_action_delete,
                iconColor = SemanticColor.iconBlack, // 검은색
                textColor = SemanticColor.iconBlack, // 검은색
                backgroundColor = SemanticColor.backgroundWhitePrimary,
                onClick = onBlockClick,
            )
        }
    }
}
