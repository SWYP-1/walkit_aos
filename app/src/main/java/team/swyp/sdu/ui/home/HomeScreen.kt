package team.swyp.sdu.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.presentation.viewmodel.UserViewModel
import team.swyp.sdu.core.DataState
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.ui.home.components.WeeklyRecordCard
import team.swyp.sdu.ui.home.components.HomeHeader
import team.swyp.sdu.ui.home.components.DominantEmotionCard
import team.swyp.sdu.ui.home.components.EmotionIcon
import team.swyp.sdu.ui.home.components.HomeEmptySession
import team.swyp.sdu.ui.home.components.ProfileSection
import team.swyp.sdu.ui.home.components.MissionSection
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 홈 화면 Route (ViewModel 주입 및 네비게이션 처리)
 */
@Composable
fun HomeRoute(
    onClickWalk: () -> Unit = {},
    onClickAlarm: () -> Unit = {},
    onClickMissionMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goalState by viewModel.goalUiState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val missionUiState by viewModel.missionUiState.collectAsStateWithLifecycle()
    val walkingSessionDataState by viewModel.walkingSessionDataState.collectAsStateWithLifecycle()
    val userState by userViewModel.userState.collectAsStateWithLifecycle()

    // 사용자 프로필 이미지 URL 추출
    val profileImageUrl = when (userState) {
        is Result.Success -> {
            (userState as Result.Success<User>).data.imageName
        }
        else -> null
    }

    HomeScreen(
        uiState = uiState,
        goalState = goalState,
        profileUiState = profileUiState,
        missionUiState = missionUiState,
        walkingSessionDataState = walkingSessionDataState,
        profileImageUrl = profileImageUrl,
        onClickWalk = onClickWalk,
        onClickAlarm = onClickAlarm,
        onRewardClick = { missionId ->
            viewModel.requestWeeklyMissionReward(missionId)
        },
        onClickMissionMore = onClickMissionMore,
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
    profileImageUrl: String? = null,
    onClickMissionMore: () -> Unit,
    onClickAlarm: () -> Unit,
    onClickMission: () -> Unit,
    onRewardClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHeader(profileImageUrl = profileImageUrl, onClickAlarm = onClickAlarm)

        // ==============================
        // 프로필 섹션
        // ==============================
        ProfileSection(
            goalState = goalState,
            uiState = profileUiState,
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
                onClickMission = onClickMission
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
    profileImageUrl: String? = null,
    onClickWalk: () -> Unit = {},
    onClickAlarm: () -> Unit = {},
    onRewardClick: (Long) -> Unit = {},
    onClickMissionMore: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HomeScreenContent(
        uiState = uiState,
        goalState = goalState,
        profileUiState = profileUiState,
        missionUiState = missionUiState,
        walkingSessionDataState = walkingSessionDataState,
        profileImageUrl = profileImageUrl,
        onClickAlarm = onClickAlarm,
        onClickMission = onClickWalk,
        onRewardClick = onRewardClick,
        onClickMissionMore = onClickMissionMore,
        onRetry = onRetry,
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
    onClickMission: () -> Unit = {}
) {

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "나의 산책 기록",

            // body XL/semibold
            style = MaterialTheme.walkItTypography.bodyXL.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = SemanticColor.textBorderPrimary
        )
    }
    Spacer(Modifier.height(12.dp))

    when (walkingSessionDataState) {
        is DataState.Success -> {
            val records = walkingSessionDataState.data.sessionsThisWeek

            Column {
                if (records.isNotEmpty()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        records.forEach { session ->
                            WeeklyRecordCard(
                                session = session,
                                modifier = Modifier.width(260.dp),
                            )
                        }
                    }
                } else {
                    HomeEmptySession(onClick = onClickMission)
                }
                Spacer(Modifier.height(32.dp))

                Text(
                    text = "나의 감정 기록",
                    // body XL/semibold
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimary
                )
                Spacer(Modifier.height(12.dp))

                // 이번주 주요 감정 카드
                val dominantEmotion = walkingSessionDataState.data.dominantEmotion
                DominantEmotionCard(
                    emotionType = dominantEmotion,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val recentEmotions = walkingSessionDataState.data.recentEmotions
                    val itemCount = recentEmotions.size.coerceAtMost(7)

                    repeat(7) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            if (index < itemCount) {
                                recentEmotions[index]?.let { emotion ->
                                    EmotionIcon(emotionType = emotion)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
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
