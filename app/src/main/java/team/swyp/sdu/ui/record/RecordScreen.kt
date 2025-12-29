package team.swyp.sdu.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.record.components.*
import team.swyp.sdu.ui.record.friendrecord.FriendRecordScreen
import team.swyp.sdu.ui.theme.SemanticColor
import java.time.LocalDate
import team.swyp.sdu.ui.record.RecordViewModel.RecordUiState
import team.swyp.sdu.ui.record.friendrecord.component.FriendRecordSkeletonRow
import team.swyp.sdu.ui.theme.walkItTypography


@Composable
fun RecordRoute(
    modifier: Modifier = Modifier,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    recordViewModel: RecordViewModel = hiltViewModel(),
    onNavigateToFriend: () -> Unit = {},
    onNavigateToAlarm: () -> Unit = {},
    onNavigateToDailyRecord: (String) -> Unit = {},
    onStartOnboarding: () -> Unit = {},
) {
    val recordUiState by recordViewModel.uiState.collectAsStateWithLifecycle()
    val weekStats by calendarViewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by calendarViewModel.monthStats.collectAsStateWithLifecycle()
    val monthSessions by calendarViewModel.monthSessions.collectAsStateWithLifecycle()
    val weekSessions by calendarViewModel.weekSessions.collectAsStateWithLifecycle()
    val currentDate by calendarViewModel.currentDate.collectAsStateWithLifecycle()

    RecordScreenContent(
        modifier = modifier,
        recordUiState = recordUiState,
        weekStats = weekStats,
        monthStats = monthStats,
        currentDate = currentDate,
        onPrevWeek = { calendarViewModel.prevWeek() },
        onNextWeek = { calendarViewModel.nextWeek() },
        onNavigateToAlarm = onNavigateToAlarm,
        onNavigateToFriend = onNavigateToFriend,
        onNavigateToDailyRecord = onNavigateToDailyRecord,
        onMyProfileClick = { recordViewModel.clearFriendSelection() },
        onFriendSelected = { recordViewModel.selectFriend(it.nickname) },
        onFriendDeselected = { recordViewModel.clearFriendSelection() },
        monthSessions = monthSessions,
        weekSessions = weekSessions,
        onMonthChanged = { calendarViewModel.setDate(it.atDay(1)) },
        onStartOnboarding = onStartOnboarding,
        onLoadFriendRecord = { friendName -> recordViewModel.selectFriend(friendName) },
        onBlockUser = { nickName -> recordViewModel.blockSelectedFriend(nickName) }
    )
}

@Composable
private fun RecordScreenContent(
    modifier: Modifier = Modifier,
    recordUiState: RecordUiState,
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
    onStartOnboarding: () -> Unit,
    onLoadFriendRecord: (String) -> Unit,
    onBlockUser: (String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = RecordTabType.entries

    // 친구 선택 시 API 호출
    if (recordUiState is RecordUiState.Success) {
        val selectedFriend = recordUiState.selectedFriendNickname
        LaunchedEffect(selectedFriend) {
            if (selectedFriend != null) {
                // API 요청: 친구 기록 가져오기
                onLoadFriendRecord(selectedFriend)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {

        RecordHeader(onClickSearch = {}, onClickAlarm = onNavigateToAlarm)
        Spacer(Modifier.height(16.dp))
        Row(Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)) {
            Text(
                text = "친구목록",
                // caption M/regular
                style = MaterialTheme.walkItTypography.captionM,
                color = SemanticColor.textBorderPrimary,
            )
        }

        Spacer(Modifier.height(8.dp))
        // ==============================
        // 상단 API 기반 영역
        // ==============================
        when (recordUiState) {
            is RecordUiState.Loading -> {
                RecordTopSectionSkeleton()
            }

            is RecordUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("상단 데이터 로딩 실패", color = MaterialTheme.colorScheme.error)
                }
            }

            is RecordUiState.Success -> {
                RecordTopSection(
                    user = recordUiState.user,
                    friends = recordUiState.friends,
                    selectedFriendNickname = recordUiState.selectedFriendNickname,
                    onMyProfileClick = onMyProfileClick,
                    onFriendSelected = onFriendSelected,
                    onNavigateToFriend = onNavigateToFriend
                )
            }
        }
    }

    Divider()

    // ==============================
    // 하단 Room 기반 영역 (항상 표시)
    // ==============================
    if (recordUiState is RecordUiState.Success && recordUiState.selectedFriendNickname != null) {
        FriendRecordScreen(
            nickname = recordUiState.selectedFriendNickname,
            onNavigateBack = onFriendDeselected,
            onBlockUser = onBlockUser,
            modifier = Modifier.weight(1f) // RecordScreen 내에서 남은 공간만 차지
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .weight(1f) // RecordTopSection 아래부터 남은 공간만 차지
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item {
                HeaderRow(
                    onDummyClick = { /*TODO*/ },
                    onStartOnboarding = onStartOnboarding
                )
            }

            item {
                RecordTabRow(
                    selectedTabIndex = tabIndex,
                    onTabSelected = { tabIndex = it }
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
                    onMonthChanged = onMonthChanged
                )
            }
        }
    }
}

