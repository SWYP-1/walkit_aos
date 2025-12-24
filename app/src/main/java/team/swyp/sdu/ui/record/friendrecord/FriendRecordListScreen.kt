package team.swyp.sdu.ui.record.friendrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.theme.SemanticColor

/**
 * 친구 기록 리스트 화면 (부모)
 *
 * 상단에 가로 스크롤 친구 목록, 하단에 선택된 친구의 기록 리스트를 표시합니다.
 * LRU 캐시를 사용하여 친구별 상태를 관리하고 스크롤 위치를 복원합니다.
 */
@Composable
fun FriendRecordListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendRecordListViewModel = hiltViewModel(),
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val selectedFriendId by viewModel.selectedFriendId.collectAsStateWithLifecycle()
    val friendStates by viewModel.friendStates.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        AppHeader(
            title = "친구 기록",
            onNavigateBack = onNavigateBack,
        )

        // 상단: 가로 스크롤 친구 목록
        FriendListHeader(
            friends = friends,
            selectedFriendId = selectedFriendId,
            onFriendSelected = viewModel::selectFriend,
            modifier = Modifier.fillMaxWidth(),
        )

        // 하단: 선택된 친구의 기록 리스트
        // selectedFriendId가 null이면 자기 화면 (친구 선택 안 함)
        // selectedFriendId가 null이 아니면 선택된 친구의 기록 표시
        selectedFriendId?.let { friendId ->
            val currentState = friendStates[friendId]
            if (currentState != null) {
                val nonNullFriendId = friendId // 명시적으로 non-null 변수 생성
                FriendRecordListContent(
                    state = currentState,
                    onScrollPositionChanged = { index, offset ->
                        viewModel.saveScrollPosition(nonNullFriendId, index, offset)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // 로딩 중
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                }
            }
        }
        // selectedFriendId가 null이면 아무것도 표시하지 않음 (자기 화면)
    }
}

/**
 * 친구 목록 헤더 (가로 스크롤)
 */
@Composable
private fun FriendListHeader(
    friends: List<Friend>,
    selectedFriendId: String?,
    onFriendSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .height(100.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(friends) { friend ->
            FriendAvatarItem(
                friend = friend,
                isSelected = friend.id == selectedFriendId,
                onClick = { onFriendSelected(friend.id) },
            )
        }
    }
}

/**
 * 친구 아바타 아이템
 */
@Composable
private fun FriendAvatarItem(
    friend: Friend,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        SemanticColor.textBorderGreenSecondary
                    } else {
                        Color(0xFFF3F3F5) // color/text-border/secondary-inverse
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            // TODO: 실제 아바타 이미지 로딩 (Coil 사용)
            Text(
                text = friend.nickname.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) Color.White else SemanticColor.iconGrey,
            )
        }
        Text(
            text = friend.nickname,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) {
                SemanticColor.textBorderGreenSecondary
            } else {
                SemanticColor.iconGrey
            },
        )
    }
}

/**
 * 친구 기록 리스트 콘텐츠
 */
@Composable
private fun FriendRecordListContent(
    state: FriendRecordState,
    onScrollPositionChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.scrollIndex,
        initialFirstVisibleItemScrollOffset = state.scrollOffset,
    )

    // 스크롤 위치 변경 감지
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onScrollPositionChanged(index, offset)
        }
    }

    when (val result = state.recordsResult) {
        is Result.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator()
            }
        }

        is Result.Success -> {
            if (result.data.isEmpty()) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "기록이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SemanticColor.iconGrey,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(result.data) { record ->
                        FriendRecordItem(record = record)
                    }
                }
            }
        }

        is Result.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "오류가 발생했습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = SemanticColor.iconGrey,
                    )
                    Text(
                        text = result.message ?: "알 수 없는 오류",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SemanticColor.iconGrey,
                    )
                }
            }
        }
    }
}

/**
 * 친구 기록 아이템
 */
@Composable
private fun FriendRecordItem(
    record: FollowerWalkRecord,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 캐릭터 정보
            if (record.character.nickName != null) {
                Text(
                    text = record.character.nickName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "레벨: ${record.character.level}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "등급: ${record.character.grade.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 산책 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "걸음 수",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticColor.iconGrey,
                    )
                    Text(
                        text = "${record.stepCount}걸음",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column {
                    Text(
                        text = "총 거리",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticColor.iconGrey,
                    )
                    Text(
                        text = "${record.totalDistance}m",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (record.walkProgressPercentage != null) {
                    Column {
                        Text(
                            text = "진행률",
                            style = MaterialTheme.typography.bodySmall,
                            color = SemanticColor.iconGrey,
                        )
                        Text(
                            text = "${record.walkProgressPercentage}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            if (record.createdDate != null) {
                Text(
                    text = "생성일: ${record.createdDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticColor.iconGrey,
                )
            }
        }
    }
}

