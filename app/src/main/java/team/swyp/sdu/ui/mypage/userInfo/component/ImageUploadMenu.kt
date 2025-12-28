package team.swyp.sdu.ui.mypage.userInfo.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 이미지 업로드 메뉴 컴포넌트
 */
@Composable
fun ImageUploadMenu(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greenPrimary = SemanticColor.stateGreenPrimary

    Box(modifier = modifier) {
        var showImageMenu by remember { mutableStateOf(false) }

        // 이미지 업로드 버튼
        Row(
            modifier = Modifier
                .clickable(onClick = { showImageMenu = true })
                .border(
                    width = 1.dp,
                    color = greenPrimary,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = greenPrimary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "이미지 업로드",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
                color = greenPrimary,
            )
        }

        // 이미지 선택 드랍다운 메뉴
        DropdownMenu(
            expanded = showImageMenu,
            onDismissRequest = { showImageMenu = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "촬영하기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {
                    showImageMenu = false
                    onCameraClick()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "갤러리에서 가져오기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {
                    showImageMenu = false
                    onGalleryClick()
                }
            )
        }
    }
}
