package swyp.team.walkit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import swyp.team.walkit.R
import swyp.team.walkit.presentation.viewmodel.UserViewModel
import swyp.team.walkit.core.DataState
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.model.User
import swyp.team.walkit.ui.home.components.WeeklyRecordCard
import swyp.team.walkit.ui.home.components.HomeHeader
import swyp.team.walkit.ui.home.components.DominantEmotionCard
import swyp.team.walkit.ui.home.components.EmotionIcon
import swyp.team.walkit.ui.home.components.HomeEmptySession
import swyp.team.walkit.ui.home.components.ProfileSection
import swyp.team.walkit.ui.home.components.MissionSection
import swyp.team.walkit.ui.home.components.WeeklyRecordSection
import swyp.team.walkit.ui.home.components.EmotionRecordSection
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.ui.walking.utils.stringToEmotionTypeOrNull

/**
 * 홈 화면 Route (ViewModel 주입 및 네비게이션 처리)
 */
@Composable
fun HomeRoute(
    onClickWalk: () -> Unit = {},
    onClickAlarm: () -> Unit = {},
    onClickMissionMore: () -> Unit = {},
    onNavigateToRecord: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goalState by viewModel.goalUiState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val missionUiState by viewModel.missionUiState.collectAsStateWithLifecycle()
    val walkingSessionDataState by viewModel.walkingSessionDataState.collectAsStateWithLifecycle()
    val characterLottieState by viewModel.characterLottieState.collectAsStateWithLifecycle() // ✅ 캐릭터 Lottie 상태 추가
    val userState by userViewModel.userState.collectAsStateWithLifecycle()

    // 사용자 프로필 이미지 URL 추출
    val profileImageUrl = when (userState) {
        is Result.Success -> {
            (userState as Result.Success<User>).data.imageName
        }

        else -> null
    }

    // 캐릭터 Lottie 표시 초기화
    LaunchedEffect(Unit) {
        viewModel.loadCharacterDisplay()
    }

    HomeScreen(
        uiState = uiState,
        goalState = goalState,
        profileUiState = profileUiState,
        missionUiState = missionUiState,
        walkingSessionDataState = walkingSessionDataState,
        characterLottieState = characterLottieState, // ✅ 캐릭터 Lottie 상태 전달
        profileImageUrl = profileImageUrl,
        onClickWalk = onClickWalk,
        onClickAlarm = onClickAlarm,
        onRewardClick = { missionId ->
            viewModel.requestWeeklyMissionReward(missionId)
        },
        onTestClick = { viewModel.cycleCharacterLevelAndGradeForTest() },
        onClickMissionMore = onClickMissionMore,
        onNavigateToRecord = onNavigateToRecord,
        modifier = modifier,
        onRetry = viewModel::loadHomeData
    )
}

/**
 * 홈 화면 Content (Section 단위 분리)
 */
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    goalState: DataState<Goal>,
    profileUiState: ProfileUiState,
    missionUiState: MissionUiState,
    walkingSessionDataState: DataState<WalkingSessionData>,
    characterLottieState: swyp.team.walkit.domain.model.LottieCharacterState?, // ✅ 캐릭터 Lottie 상태 추가
    profileImageUrl: String? = null,
    onClickMissionMore: () -> Unit,
    onClickAlarm: () -> Unit,
    onClickWalk : () -> Unit,
    onClickMission: () -> Unit,
    onRewardClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onNavigateToRecord : () -> Unit,
    onTestClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHeader(profileImageUrl = profileImageUrl, onClickAlarm = onClickAlarm)

        // ==============================
        // 프로필 섹션
        // ==============================
        ProfileSection(
            goalState = goalState,
            uiState = profileUiState,
            characterLottieState = characterLottieState, // ✅ 캐릭터 Lottie 상태 전달
            onTestClick = onTestClick, // 테스트용 클릭 핸들러
            modifier = Modifier.fillMaxWidth(),
            onRetry = onRetry
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 12.dp),
        ) {
            // ==============================
            // 미션 섹션
            // ==============================
            MissionSection(
                uiState = missionUiState,
                onClickMission = onClickMission,
                onRewardClick = onRewardClick,
                onClickMissionMore = onClickMissionMore,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ==============================
            // 하단 Room 기반 영역 (항상 표시)
            // ==============================
            HomeBottomSection(
                walkingSessionDataState = walkingSessionDataState,
                onClickMore = onNavigateToRecord,
                onClickWalk = onClickWalk,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * 홈 화면 (프로필/미션/주간 기록 섹션 분리)
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    goalState: DataState<Goal>,
    profileUiState: ProfileUiState,
    missionUiState: MissionUiState,
    walkingSessionDataState: DataState<WalkingSessionData>,
    characterLottieState: swyp.team.walkit.domain.model.LottieCharacterState?, // ✅ 캐릭터 Lottie 상태 추가
    profileImageUrl: String? = null,
    onClickWalk: () -> Unit = {},
    onClickAlarm: () -> Unit = {},
    onRewardClick: (Long) -> Unit = {},
    onClickMissionMore: () -> Unit = {},
    onNavigateToRecord : () -> Unit = {},
    onRetry: () -> Unit = {},
    onTestClick : () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HomeScreenContent(
        uiState = uiState,
        goalState = goalState,
        profileUiState = profileUiState,
        missionUiState = missionUiState,
        walkingSessionDataState = walkingSessionDataState,
        characterLottieState = characterLottieState, // ✅ 캐릭터 Lottie 상태 전달
        profileImageUrl = profileImageUrl,
        onClickAlarm = onClickAlarm,
        onClickMission = onClickWalk,
        onRewardClick = onRewardClick,
        onClickMissionMore = onClickMissionMore,
        onRetry = onRetry,
        onNavigateToRecord = onNavigateToRecord,
        onClickWalk = onClickWalk,
        onTestClick = onTestClick,
        modifier = modifier,
    )
}

// ==============================
// Section 기반 컴포넌트들 (토스/배민 스타일)
// ==============================

// ==============================
// 하단 Room 기반 섹션 컴포넌트들
// ==============================

/**
 * 하단 Room 기반 영역 (항상 표시)
 */
@Composable
private fun HomeBottomSection(
    walkingSessionDataState: DataState<WalkingSessionData>,
    onClickMore: () -> Unit = {},
    onClickWalk : () -> Unit = {},
) {
    when (walkingSessionDataState) {
        is DataState.Success -> {
            val data = walkingSessionDataState.data

            Column {
                // 주간 기록 섹션
                WeeklyRecordSection(
                    records = data.sessionsThisWeek,
                    onClickMore = onClickMore,
                    onClickWalk = onClickWalk,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                // 감정 기록 섹션 (String을 EmotionType으로 변환)
                EmotionRecordSection(
                    dominantEmotion = stringToEmotionTypeOrNull(data.dominantEmotion),
                    dominantEmotionCount = data.dominantEmotionCount,
                    recentEmotions = data.recentEmotions.mapNotNull { emotionString ->
                        stringToEmotionTypeOrNull(emotionString)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is DataState.Error -> {
            // Room 데이터 로딩 실패 (API와 별개로 처리)
            Text(
                text = "산책 기록을 불러올 수 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        DataState.Loading -> {
            // Room 데이터 로딩 중 (API와 별개로 처리)
            Text(
                text = "산책 기록을 불러오는 중...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
