package swyp.team.walkit.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import swyp.team.walkit.domain.model.Friend
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.presentation.viewmodel.CalendarViewModel
import swyp.team.walkit.presentation.viewmodel.CalendarViewModel.WalkAggregate
import swyp.team.walkit.presentation.viewmodel.GoalViewModel
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.record.components.*
import swyp.team.walkit.ui.record.friendrecord.FriendRecordScreen
import swyp.team.walkit.ui.theme.SemanticColor
import java.time.LocalDate
import swyp.team.walkit.ui.record.RecordViewModel.RecordUiState
import swyp.team.walkit.ui.record.friendrecord.FriendRecordViewModel
import swyp.team.walkit.ui.record.friendrecord.component.FriendRecordSkeletonRow
import swyp.team.walkit.ui.theme.walkItTypography
import timber.log.Timber


@Composable
fun RecordRoute(
    modifier: Modifier = Modifier,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    recordViewModel: RecordViewModel = hiltViewModel(),
    goalViewModel: GoalViewModel = hiltViewModel(),
    friendBarViewModel: FriendBarViewModel = hiltViewModel(),
    friendRecordViewModel: FriendRecordViewModel = hiltViewModel(),
    onNavigateToFriend: () -> Unit = {},
    onNavigateToAlarm: () -> Unit = {},
    onNavigateToDailyRecord: (String) -> Unit = {},
    onStartOnboarding: () -> Unit = {},
    onLoadFriendRecord: (String) -> Unit = {}, // 더 이상 사용하지 않음
) {
    val recordUiState by recordViewModel.uiState.collectAsStateWithLifecycle()
    val goalState by goalViewModel.goalState.collectAsStateWithLifecycle()
    val goal = (goalState as? swyp.team.walkit.core.Result.Success)?.data ?: Goal.EMPTY
    val friendsState by friendBarViewModel.friendsState.collectAsStateWithLifecycle()
    val weekStats by calendarViewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by calendarViewModel.monthStats.collectAsStateWithLifecycle()
    val monthSessions by calendarViewModel.monthSessions.collectAsStateWithLifecycle()
    val weekSessions by calendarViewModel.weekSessions.collectAsStateWithLifecycle()
    val monthMissionsCompleted by calendarViewModel.monthMissionsCompleted.collectAsStateWithLifecycle()
    val currentDate by calendarViewModel.currentDate.collectAsStateWithLifecycle()
    val missionProgress by calendarViewModel.missionProgress.collectAsStateWithLifecycle()

    // RecordScreen 진입 시 친구 목록 캐시 확인 및 갱신
    LaunchedEffect(Unit) {
        Timber.d("📱 RecordScreen 진입 - 친구 목록 캐시 확인 및 갱신")
        friendBarViewModel.refreshFriendsIfNeeded()
    }

    // 디버그: 데이터 상태 확인
    LaunchedEffect(monthSessions, weekSessions) {
        Timber.d("📊 RecordScreen 데이터 상태 - monthSessions: ${monthSessions.size}개, weekSessions: ${weekSessions.size}개")
        if (monthSessions.isNotEmpty()) {
            Timber.d("🎯 RecordScreen 첫 번째 monthSession: ${monthSessions.first().startTime}")
        }
    }

    RecordScreenContent(
        modifier = modifier,
        recordUiState = recordUiState,
        goal = goal,
        friendsState = friendsState,
        weekStats = weekStats,
        monthStats = monthStats,
        currentDate = currentDate,
        missionProgress = missionProgress,
        onPrevWeek = { calendarViewModel.prevWeek() },
        onNextWeek = { calendarViewModel.nextWeek() },
        onNavigateToAlarm = onNavigateToAlarm,
        onNavigateToFriend = onNavigateToFriend,
        onNavigateToDailyRecord = onNavigateToDailyRecord,
        onMyProfileClick = {
            recordViewModel.clearFriendSelection()
        },
        onFriendSelected = { friend ->
            recordViewModel.selectFriend(friend.nickname)
        },
        onFriendDeselected = { recordViewModel.clearFriendSelection() },
        monthSessions = monthSessions,
        weekSessions = weekSessions,
        monthMissionsCompleted = monthMissionsCompleted,
        onMonthChanged = { calendarViewModel.setDate(it.atDay(1)) },
        onBlockUser = { nickName -> recordViewModel.blockSelectedFriend(nickName) },
        onSetDate = { today -> calendarViewModel.setDate(today) },
        onClaimMissionReward = { id -> calendarViewModel.claimMissionReward(id) },
        onUpdateNote = { id, note -> calendarViewModel.updateSessionNote(id, note) },
        onDeleteNote = { id -> calendarViewModel.deleteSessionNote(id) },
    )
}

@Composable
private fun RecordScreenContent(
    modifier: Modifier = Modifier,
    recordUiState: RecordUiState,
    goal: Goal,
    friendsState: swyp.team.walkit.core.Result<List<Friend>>,
    weekStats: WalkAggregate,
    monthStats: WalkAggregate,
    currentDate: LocalDate,
    missionProgress: swyp.team.walkit.domain.model.MissionProgress,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToFriend: () -> Unit,
    onNavigateToAlarm: () -> Unit,
    onNavigateToDailyRecord: (String) -> Unit,
    onMyProfileClick: () -> Unit,
    onFriendSelected: (Friend) -> Unit,
    onFriendDeselected: () -> Unit,
    monthSessions: List<swyp.team.walkit.data.model.WalkingSession>,
    weekSessions: List<swyp.team.walkit.data.model.WalkingSession>,
    monthMissionsCompleted: List<String>,
    onMonthChanged: (java.time.YearMonth) -> Unit,
    onSetDate: (LocalDate) -> Unit,
    onBlockUser: (String) -> Unit,
    onClaimMissionReward: (Long) -> Unit = {},
    onUpdateNote: (id: String, note: String) -> Unit = { _, _ -> },
    onDeleteNote: (id: String) -> Unit = {},
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = RecordTabType.values()

    // 탭 변경 시 currentDate 리셋 (데이터 일관성 유지)
    LaunchedEffect(tabIndex) {
        // 모든 탭에서 현재 날짜로 리셋하여 데이터 표시 일관성 확보
        val today = LocalDate.now()
//        calendarViewModel.setDate(today)
        onSetDate(today)
        Timber.d("📅 RecordScreen 탭 변경: ${tabs[tabIndex].name} → currentDate 리셋: $today")
    }

    // 스크롤 상태
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhiteSecondary)
    ) {

        // 상단 스크롤 영역
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // 상단 영역들
            RecordHeader(onClickAlarm = onNavigateToAlarm)
//            Spacer(Modifier.height(16.dp))
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 20.dp)
//            ) {
//                Text(
//                    text = "친구목록",
//                    style = MaterialTheme.walkItTypography.captionM,
//                    color = SemanticColor.textBorderPrimary,
//                )
//            }
//            Spacer(Modifier.height(8.dp))
//
//            // 상단 API 기반 영역
//            when (recordUiState) {
//                is RecordUiState.Loading -> {
//                    RecordTopSectionSkeleton()
//                }
//
//                is RecordUiState.Error -> {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(120.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("상단 데이터 로딩 실패", color = MaterialTheme.colorScheme.error)
//                    }
//                }
//
//                is RecordUiState.Success -> {
//                    // FriendBarViewModel에서 친구 목록 상태 가져오기
//                    val friends = when (friendsState) {
//                        is swyp.team.walkit.core.Result.Success -> friendsState.data
//                        else -> emptyList()
//                    }
//
//                    RecordTopSection(
//                        user = recordUiState.user,
//                        friends = friends,
//                        selectedFriendNickname = recordUiState.selectedFriendNickname,
//                        onMyProfileClick = onMyProfileClick,
//                        onFriendSelected = onFriendSelected,
//                        onNavigateToFriend = onNavigateToFriend
//                    )
//                }
//            }
//
//            Divider()

            // 친구 미선택 시 탭 콘텐츠 표시
            if (!(recordUiState is RecordUiState.Success && recordUiState.selectedFriendNickname != null)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SemanticColor.backgroundWhiteSecondary)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    RecordTabRow(
                        selectedTabIndex = tabIndex,
                        onTabSelected = { tabIndex = it }
                    )
                    Spacer(Modifier.height(24.dp))

                    RecordTabContent(
                        selectedTab = tabs[tabIndex],
                        monthStats = monthStats,
                        weekStats = weekStats,
                        monthSessions = monthSessions,
                        weekSessions = weekSessions,
                        monthMissionsCompleted = monthMissionsCompleted,
                        currentDate = currentDate,
                        goal = goal,
                        missionProgress = missionProgress,
                        onPrevWeek = onPrevWeek,
                        onNextWeek = onNextWeek,
                        onNavigateToDailyRecord = onNavigateToDailyRecord,
                        onMonthChanged = onMonthChanged,
                        onClaimMissionReward = onClaimMissionReward,
                        onUpdateNote = onUpdateNote,
                        onDeleteNote = onDeleteNote,
                    )


                    // 최소 높이 확보 (스크롤 가능하도록)
                    Spacer(Modifier.height(50.dp))
                }
            } else {
                // 친구 선택 시 하단 전체 화면 영역 (스크롤 영역 밖)
                FriendRecordScreen(
                    nickname = recordUiState.selectedFriendNickname,
                    onNavigateBack = onFriendDeselected,
                    onBlockUser = onBlockUser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 600.dp) // 로딩 바 중앙 표시를 위한 최소 높이 보장
                )
            }
        }


    }
}