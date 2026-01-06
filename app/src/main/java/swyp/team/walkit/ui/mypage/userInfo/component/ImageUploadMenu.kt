package swyp.team.walkit.ui.mypage.userInfo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 이미지 업로드 메뉴 컴포넌트
 */
@Composable
fun ImageUploadMenu(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    showDeleteOption: Boolean = true,
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
            modifier = Modifier.background(SemanticColor.backgroundWhitePrimary) // 드롭다운 메뉴 배경색 변경
        ) {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_camera),
                        contentDescription = "camera",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "촬영하기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {
                    showImageMenu = false
                    onCameraClick()
                },
                modifier = modifier.height(32.dp)
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_gallery),
                        contentDescription = "gallery",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "갤러리에서 가져오기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {
                    showImageMenu = false
                    onGalleryClick()
                },
                modifier = modifier.height(32.dp)
            )
            if (showDeleteOption) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_minus),
                            tint = SemanticColor.iconBlack,
                            contentDescription = "minus",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "삭제하기",
                            style = MaterialTheme.walkItTypography.bodyM,
                        )
                    },
                    onClick = {
                        showImageMenu = false
                        onDeleteClick()
                    },
                    modifier = modifier.height(32.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "이미지 업로드 메뉴 (삭제하기 포함)")
@Composable
private fun ImageUploadMenuPreview() {
    WalkItTheme {
        ImageUploadMenu(
            onCameraClick = {},
            onGalleryClick = {},
            onDeleteClick = {},
            showDeleteOption = true
        )
    }
}

@Preview(showBackground = true, name = "이미지 업로드 메뉴 (삭제하기 제외)")
@Composable
private fun ImageUploadMenuWithoutDeletePreview() {
    WalkItTheme {
        ImageUploadMenu(
            onCameraClick = {},
            onGalleryClick = {},
            onDeleteClick = {},
            showDeleteOption = false
        )
    }
}

@Preview(showBackground = true, name = "드롭다운 메뉴 아이템들")
@Composable
private fun DropdownMenuItemsPreview() {
    WalkItTheme {
        androidx.compose.material3.DropdownMenu(
            expanded = true,
            onDismissRequest = {},
            modifier = Modifier.background(SemanticColor.backgroundWhitePrimary)
        ) {
            androidx.compose.material3.DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_camera),
                        contentDescription = "camera"
                    )
                },
                text = {
                    Text(
                        text = "촬영하기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {}
            )
            androidx.compose.material3.DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_gallery),
                        contentDescription = "gallery"
                    )
                },
                text = {
                    Text(
                        text = "갤러리에서 가져오기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {}
            )
            androidx.compose.material3.DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_minus),
                        contentDescription = "delete",
                        tint = SemanticColor.iconBlack
                    )
                },
                text = {
                    Text(
                        text = "삭제하기",
                        style = MaterialTheme.walkItTypography.bodyM,
                    )
                },
                onClick = {}
            )
        }
    }
}
