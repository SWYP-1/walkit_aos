package team.swyp.sdu.ui.record

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import coil.compose.AsyncImage
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.model.User
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
    modifier: Modifier = Modifier,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    recordViewModel: RecordViewModel = hiltViewModel(),
    onNavigateToFriend: () -> Unit = {},
    onNavigateToAlarm: () -> Unit = {},
    onNavigateToDailyRecord: (String) -> Unit = {}, // 날짜 형식: "yyyy-MM-dd"
    onStartOnboarding: () -> Unit = {},
) {
    val recordUiState by recordViewModel.uiState.collectAsStateWithLifecycle()
    val dummyMessage by calendarViewModel.dummyMessage.collectAsStateWithLifecycle()
    val weekStats by calendarViewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by calendarViewModel.monthStats.collectAsStateWithLifecycle()
    val monthSessions by calendarViewModel.monthSessions.collectAsStateWithLifecycle()
    val weekSessions by calendarViewModel.weekSessions.collectAsStateWithLifecycle()
    val currentDate by calendarViewModel.currentDate.collectAsStateWithLifecycle()

    RecordScreenContent(
        modifier = modifier,
        recordUiState = recordUiState,
        dummyMessage = dummyMessage,
        onDummyClick = { calendarViewModel.generateDummyData() },
        onStartOnboarding = onStartOnboarding,
        weekStats = weekStats,
        monthStats = monthStats,
        currentDate = currentDate,
        onPrevWeek = { calendarViewModel.prevWeek() },
        onNextWeek = { calendarViewModel.nextWeek() },
        onNavigateToAlarm = onNavigateToAlarm,
        onNavigateToFriend = onNavigateToFriend,
        onNavigateToDailyRecord = onNavigateToDailyRecord,
        onMyProfileClick = {
            // 내 프로필 클릭 시 캘린더 화면으로 이동 (현재 화면이 캘린더 화면이므로 선택 해제)
            recordViewModel.clearFriendSelection()
        },
        onFriendSelected = { friend ->
            recordViewModel.selectFriend(friend.nickname)
        },
        onFriendDeselected = {
            recordViewModel.clearFriendSelection()
        },
        monthSessions = monthSessions,
        weekSessions = weekSessions,
        onMonthChanged = { yearMonth ->
            // 월 변경 시 CalendarViewModel의 날짜를 해당 월의 첫 날로 설정하여 쿼리 트리거
            calendarViewModel.setDate(yearMonth.atDay(1))
        },
    )
}

@Composable
private fun RecordScreenContent(
    modifier: Modifier = Modifier,
    recordUiState: RecordUiState,
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
    onNavigateToDailyRecord: (String) -> Unit,
    onMyProfileClick: () -> Unit,
    onFriendSelected: (Friend) -> Unit,
    onFriendDeselected: () -> Unit,
    monthSessions: List<team.swyp.sdu.data.model.WalkingSession>,
    weekSessions: List<team.swyp.sdu.data.model.WalkingSession>,
    onMonthChanged: (java.time.YearMonth) -> Unit,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = RecordTabType.entries

    when (val state = recordUiState) {
        is RecordUiState.Loading -> {
            // 로딩 상태 처리 (필요시 로딩 UI 표시)
        }
        is RecordUiState.Error -> {
            // 에러 상태 처리
        }
        is RecordUiState.Success -> {
            Column(
                modifier = modifier.fillMaxSize(),
            ) {
                // Header
                RecordHeader(
                    onClickAlarm = { onNavigateToAlarm() },
                    onClickSearch = { onNavigateToFriend() }
                )

                // 친구 목록 가로 스크롤
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 내 프로필 이미지
                    MyProfileImage(
                        user = state.user,
                        onClick = onMyProfileClick,
                        isSelected = state.selectedFriendNickname == null,
                    )

                    FriendListRow(
                        friends = state.friends,
                        selectedFriendNickname = state.selectedFriendNickname,
                        onFriendSelected = onFriendSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 구분선
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                )

                // 하단 영역: 친구가 선택되었으면 FriendRecordRoute, 아니면 기존 탭 내용
                if (state.selectedFriendNickname != null) {
                    FriendRecordRoute(
                        nickname = state.selectedFriendNickname!!,
                        onNavigateBack = onFriendDeselected,
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
                        selectedTab = tabs[tabIndex],
                        monthStats = monthStats,
                        weekStats = weekStats,
                        monthSessions = monthSessions,
                        weekSessions = weekSessions,
                        currentDate = currentDate,
                        onPrevWeek = onPrevWeek,
                        onNextWeek = onNextWeek,
                        onNavigateToDailyRecord = onNavigateToDailyRecord,
                        onMonthChanged = onMonthChanged,
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
    }
}

/**
 * 내 프로필 이미지 컴포넌트
 */
@Composable
private fun MyProfileImage(
    user: User?,
    onClick: () -> Unit,
    isSelected: Boolean,
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
            AsyncImage(
                model = user?.imageName,
                contentDescription = "my profile image",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                placeholder = painterResource(R.drawable.ic_default_user),
                error = painterResource(R.drawable.ic_default_user),
            )
        }
        Text(
            text = user?.nickname ?: "나",
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
    selectedTab: RecordTabType,
    monthStats: WalkAggregate,
    weekStats: WalkAggregate,
    monthSessions: List<team.swyp.sdu.data.model.WalkingSession>,
    weekSessions: List<team.swyp.sdu.data.model.WalkingSession>,
    currentDate: LocalDate,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToDailyRecord: (String) -> Unit,
    onMonthChanged: (java.time.YearMonth) -> Unit,
) {
    when (selectedTab) {
        RecordTabType.Month -> MonthSection(
            stats = monthStats,
            sessions = monthSessions,
            onNavigateToDailyRecord = onNavigateToDailyRecord,
            onMonthChanged = onMonthChanged,
        )
        RecordTabType.Week -> WeekSection(
            stats = weekStats,
            currentDate = currentDate,
            onPrevWeek = onPrevWeek,
            onNextWeek = onNextWeek,
            sessions = weekSessions,
        )
    }
}
