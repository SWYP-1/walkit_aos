package team.swyp.sdu.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.SearchBar
import team.swyp.sdu.ui.friend.FriendViewModel
import team.swyp.sdu.ui.theme.Pretendard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.TypeScale
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 친구 목록 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun FriendScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: FriendViewModel = hiltViewModel(),
) {
    val filteredFriends by viewModel.filteredFriends.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    FriendScreenContent(
        filteredFriends = filteredFriends,
        query = query,
        onNavigateBack = onNavigateBack,
        onNavigateToSearch = onNavigateToSearch,
        onQueryChange = viewModel::updateQuery,
        onClearQuery = viewModel::clearQuery,
        onBlockFriend = viewModel::blockFriend,
    )
}

/**
 * 친구 목록 화면 Content
 *
 * 실제 UI 컴포넌트를 렌더링합니다.
 */
@Composable
private fun FriendScreenContent(
    filteredFriends: List<Friend>,
    query: String,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBlockFriend: (String) -> Unit,
) {
    var menuTargetId by remember { mutableStateOf<String?>(null) }
    var confirmTarget by remember { mutableStateOf<Friend?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppHeader(title = "친구목록", onNavigateBack = onNavigateBack, rightAction = {
            IconButton(
                onClick = { onNavigateToSearch() },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_plus),
                    contentDescription = "친구 추가로 이동",
                    tint = SemanticColor.iconBlack,
                    modifier = Modifier.size(24.dp),
                )
            }
        })

        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onClear = onClearQuery,
            modifier = Modifier.padding(16.dp)
        )
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${filteredFriends.size}",
                // body S/medium
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderGreenSecondary
            )
            Spacer(Modifier.width(4.dp))
            // body S/regular
            Text(
                text = "명",
                style = MaterialTheme.walkItTypography.bodyS,
                color = SemanticColor.iconGrey
            )
        }
        // 친구 목록 화면 (로컬 필터링)
        FriendListScreen(
            friends = filteredFriends,
            menuTargetId = menuTargetId,
            onMoreClick = { friend -> menuTargetId = friend.id },
            onMenuDismiss = { menuTargetId = null },
            onBlockClick = { friend ->
                confirmTarget = friend
                menuTargetId = null
            },
            contentPaddingBottom =
                with(LocalDensity.current) {
                    WindowInsets.navigationBars.getBottom(this).toDp()
                },
        )
    }

    confirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            title = { Text("친구 차단하기") },
            text = { Text("${target.nickname} 을(를) 차단하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBlockFriend(target.id)
                        confirmTarget = null
                    },
                ) { Text("네") }
            },
            dismissButton = {
                TextButton(onClick = { confirmTarget = null }) { Text("아니오") }
            },
        )
    }
}

/**
 * 친구 목록 화면
 */
@Composable
private fun FriendListScreen(
    friends: List<Friend>,
    menuTargetId: String?,
    onMoreClick: (Friend) -> Unit,
    onMenuDismiss: () -> Unit,
    onBlockClick: (Friend) -> Unit,
    contentPaddingBottom: Dp,
) {
    Surface(
        tonalElevation = 0.dp,
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = contentPaddingBottom + 12.dp),
        ) {
            items(friends) { friend ->
                FriendRow(
                    friend = friend,
                    menuOpen = menuTargetId == friend.id,
                    onMoreClick = onMoreClick,
                    onMenuDismiss = onMenuDismiss,
                    onBlockClick = onBlockClick,
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    menuOpen: Boolean,
    onMoreClick: (Friend) -> Unit,
    onMenuDismiss: () -> Unit,
    onBlockClick: (Friend) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFFFF)) // color/background/whtie-primary
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 왼쪽: 프로필 이미지 + 닉네임
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 프로필 이미지 (36x36, 원형)
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFFF3F3F5), // color/text-border/secondary-inverse
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    // TODO: 이미지 로딩 구현 시 Coil 사용
                    // 현재는 placeholder로 빈 원형 배경만 표시
                }

                // 닉네임
                Text(
                    text = friend.nickname,
                    fontFamily = Pretendard,
                    fontSize = TypeScale.BodyM, // 16sp
                    fontWeight = FontWeight.Medium, // Medium
                    lineHeight = (TypeScale.BodyM.value * 1.5f).sp, // lineHeight 1.5
                    letterSpacing = (-0.16f).sp, // letterSpacing -0.16px
                    color = Color(0xFF191919), // color/text-border/primary
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 오른쪽: 더보기 아이콘
            Box {
                IconButton(onClick = { onMoreClick(friend) }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "더보기",
                        tint = Color(0xFF818185), // grey
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = onMenuDismiss,
                ) {
                    DropdownMenuItem(
                        text = { Text("친구 차단하기") },
                        onClick = { onBlockClick(friend) },
                    )
                }
            }
        }

        // 하단 Divider
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xFFF3F3F5), // color/text-border/secondary-inverse
            thickness = 1.dp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendScreenPreview() {
    WalkItTheme {
        FriendScreenContent(
            filteredFriends = listOf(
                Friend("1", "닉네임 01"),
                Friend("2", "닉네임 02"),
                Friend("3", "닉네임 03"),
                Friend("4", "닉네임 04"),
                Friend("5", "닉네임 05"),
            ),
            query = "",
            onNavigateBack = {},
            onNavigateToSearch = {},
            onQueryChange = {},
            onClearQuery = {},
            onBlockFriend = {},
        )
    }
}

@Preview(showBackground = true, name = "DropdownMenu 열린 상태")
@Composable
private fun FriendScreenWithDropdownPreview() {
    WalkItTheme {
        val friends = listOf(
            Friend("1", "닉네임 01"),
            Friend("2", "닉네임 02"),
            Friend("3", "닉네임 03"),
            Friend("4", "닉네임 04"),
            Friend("5", "닉네임 05"),
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader(
                title = "친구목록",
                onNavigateBack = {},
                rightAction = {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_plus),
                            contentDescription = "친구 추가로 이동",
                            tint = SemanticColor.iconBlack,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            )

            SearchBar(
                query = "",
                onQueryChange = {},
                onClear = {},
                modifier = Modifier.padding(16.dp)
            )
            
            Row(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${friends.size}",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SemanticColor.textBorderGreenSecondary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "명",
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = SemanticColor.iconGrey
                )
            }
            
            // DropdownMenu가 열린 상태의 친구 목록
            FriendListScreen(
                friends = friends,
                menuTargetId = "2", // 두 번째 친구의 메뉴가 열린 상태
                onMoreClick = {},
                onMenuDismiss = {},
                onBlockClick = {},
                contentPaddingBottom = 0.dp,
            )
        }
    }
}
