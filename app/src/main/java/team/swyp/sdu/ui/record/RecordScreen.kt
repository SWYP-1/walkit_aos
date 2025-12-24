package team.swyp.sdu.ui.record

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.ui.record.components.HeaderRow
import team.swyp.sdu.ui.record.components.MonthSection
import team.swyp.sdu.ui.record.components.RecordHeader
import team.swyp.sdu.ui.record.components.RecordTabRow
import team.swyp.sdu.ui.record.components.RecordTabType
import team.swyp.sdu.ui.record.components.WeekSection
import team.swyp.sdu.ui.record.friendrecord.FriendRecordRoute
import team.swyp.sdu.ui.theme.SemanticColor
import java.time.LocalDate

@Composable
fun RecordRoute(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateToFriend: () -> Unit = {},
    onNavigateToAlarm: () -> Unit = {},
    onStartOnboarding: () -> Unit = {},
) {
    val dummyMessage by viewModel.dummyMessage.collectAsStateWithLifecycle()
    val dayStats by viewModel.dayStats.collectAsStateWithLifecycle()
    val weekStats by viewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by viewModel.monthStats.collectAsStateWithLifecycle()
    val daySessions by viewModel.daySessions.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    RecordScreenContent(
        navController = navController,
        modifier = modifier,
        dummyMessage = dummyMessage,
        onDummyClick = { viewModel.generateDummyData() },
        onStartOnboarding = onStartOnboarding,
        weekStats = weekStats,
        monthStats = monthStats,
        currentDate = currentDate,
        onPrevWeek = { viewModel.prevWeek() },
        onNextWeek = { viewModel.nextWeek() },
        onNavigateToAlarm = onNavigateToAlarm,
        onNavigateToFriend = onNavigateToFriend,
        allSessions = allSessions,
    )
}

@Composable
private fun RecordScreenContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    dummyMessage: String?,
    onDummyClick: () -> Unit,
    onStartOnboarding: () -> Unit,
    weekStats: WalkAggregate,
    monthStats: WalkAggregate,
    currentDate: LocalDate,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToFriend: () -> Unit,
    onNavigateToAlarm: () -> Unit,
    allSessions: List<team.swyp.sdu.data.model.WalkingSession>,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = RecordTabType.entries
    
    // 친구 선택 상태
    var selectedFriendNickname by remember { mutableStateOf<String?>(null) }
    
    // Mock 친구 목록 데이터 (Domain 모델 사용)
    val friends = remember {
        listOf(
            Friend("1", "친구1", null),
            Friend("2", "친구2", null),
            Friend("3", "친구3", null),
            Friend("4", "친구4", null),
            Friend("5", "친구5", null),
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Header
        RecordHeader(
            onClickAlarm = { onNavigateToAlarm() },
            onClickSearch = { onNavigateToFriend() }
        )
        
        // 친구 목록 가로 스크롤
        FriendListRow(
            friends = friends,
            selectedFriendNickname = selectedFriendNickname,
            onFriendSelected = { friend ->
                selectedFriendNickname = friend.nickname
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        
        // 구분선
        Divider(
            modifier = Modifier.fillMaxWidth(),
        )
        
        // 하단 영역: 친구가 선택되었으면 FriendRecordRoute, 아니면 기존 탭 내용
        if (selectedFriendNickname != null) {
            FriendRecordRoute(
                nickname = selectedFriendNickname!!,
                onNavigateBack = { selectedFriendNickname = null },
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                item { HeaderRow(onDummyClick = onDummyClick, onStartOnboarding = onStartOnboarding) }

                item {
                    RecordTabRow(
                        selectedTabIndex = tabIndex,
                        onTabSelected = { tabIndex = it },
                    )
                }

                item {
                    RecordTabContent(
                        navController = navController,
                        selectedTab = tabs[tabIndex],
                        monthStats = monthStats,
                        weekStats = weekStats,
                        allSessions = allSessions,
                        currentDate = currentDate,
                        onPrevWeek = onPrevWeek,
                        onNextWeek = onNextWeek,
                    )
                }

                item {
                    dummyMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

/**
 * 친구 목록 가로 스크롤 컴포넌트
 */
@Composable
private fun FriendListRow(
    friends: List<Friend>,
    selectedFriendNickname: String?,
    onFriendSelected: (Friend) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(friends) { friend ->
            FriendAvatarItem(
                friend = friend,
                isSelected = friend.nickname == selectedFriendNickname,
                onClick = { onFriendSelected(friend) },
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
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (friend.avatarUrl != null && friend.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.nickname,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(R.drawable.ic_default_user),
                    error = painterResource(R.drawable.ic_default_user),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_default_user),
                    contentDescription = friend.nickname,
                    modifier = Modifier.size(56.dp),
                )
            }
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
 * 기록 탭 내용 컴포넌트
 */
@Composable
private fun RecordTabContent(
    navController: NavHostController,
    selectedTab: RecordTabType,
    monthStats: WalkAggregate,
    weekStats: WalkAggregate,
    allSessions: List<team.swyp.sdu.data.model.WalkingSession>,
    currentDate: LocalDate,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    when (selectedTab) {
        RecordTabType.Month -> MonthSection(
            navController = navController,
            stats = monthStats,
            sessions = allSessions
        )
        RecordTabType.Week -> WeekSection(
            stats = weekStats,
            currentDate = currentDate,
            onPrevWeek = onPrevWeek,
            onNextWeek = onNextWeek,
            sessions = allSessions,
        )
    }
}
