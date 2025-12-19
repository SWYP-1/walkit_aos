package team.swyp.sdu.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.ui.home.HomeUiState
import team.swyp.sdu.ui.home.components.GoalCard
import team.swyp.sdu.ui.home.components.MissionCard
import team.swyp.sdu.ui.home.components.TopPill
import team.swyp.sdu.ui.home.components.LevelChip
import team.swyp.sdu.ui.home.components.WeeklyRecordCard
import team.swyp.sdu.ui.home.components.HomeMission
import team.swyp.sdu.ui.home.components.HomeHeader
import team.swyp.sdu.ui.home.components.DominantEmotionCard
import team.swyp.sdu.ui.home.components.EmotionIcon
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.home.components.CharacterSection

/**
 * 홈 화면 (요약/캐릭터/미션/주간 기록)
 */
@Composable
fun HomeScreen(
    onClickWalk: () -> Unit,
    onClickGoal: () -> Unit,
    onClickMission: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val nickname = when (uiState) {
        is HomeUiState.Success -> (uiState as HomeUiState.Success).nickname
        else -> "사용자"
    }
    val levelLabel = when (uiState) {
        is HomeUiState.Success -> (uiState as HomeUiState.Success).levelLabel
        else -> "새싹 Lv.1"
    }
    val todaySteps = when (uiState) {
        is HomeUiState.Success -> (uiState as HomeUiState.Success).todaySteps
        else -> 0
    }
    // Default dummy
    val profileImageUrl = "https://images.pexels.com/photos/3861976/pexels-photo-3861976.jpeg?_gl=1*8iaqp3*_ga*ODU3MTU1NTU2LjE3NjYwMzk4MDQ.*_ga_8JE65Q40S6*czE3NjYwMzk4MDQkbzEkZzEkdDE3NjYwMzk4MzEkajMzJGwwJGgw"
    val goalTitle = "목표명 / 달성률"
    val progressRatio = 0.68f
    val missions =
        listOf(
            HomeMission("5,000보 이상 걷기", "300 Exp"),
            HomeMission("5,000보 이상 걷기", "300 Exp"),
        )

    Column {
        HomeHeader(profileImageUrl = profileImageUrl)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                LevelChip(text = levelLabel)
            }

            CharacterSection(
                onClickWalk = onClickWalk,
                todaySteps = todaySteps,
            )

            GoalCard(
                title = goalTitle,
                progress = progressRatio,
                onClickGoal = onClickGoal,
            )

            Text(
                text = "오늘의 추천 미션",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                missions.chunked(2).forEach { rowMissions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowMissions.forEach { mission ->
                            MissionCard(
                                mission = mission,
                                modifier = Modifier.weight(1f),
                                onClick = onClickMission,
                            )
                        }
                        if (rowMissions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Text(
                text = "이번주 산책 기록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            when (uiState) {
                is HomeUiState.Success -> {
                    val records = (uiState as HomeUiState.Success).sessionsThisWeek
                    Column {
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

                        Spacer(Modifier.height(8.dp))

                        // 이번주 주요 감정 카드
                        val dominantEmotion = (uiState as HomeUiState.Success).dominantEmotion
                        if (dominantEmotion != null) {
                            DominantEmotionCard(
                                emotionType = dominantEmotion,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            Arrangement.spacedBy(4.dp)
                        ) {
                            val recentEmotions = (uiState as HomeUiState.Success).recentEmotions
                            val itemCount = recentEmotions.size.coerceAtMost(7)

                            repeat(itemCount) { index ->
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

                is HomeUiState.Error -> {
                    Text(
                        text = (uiState as HomeUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                HomeUiState.Loading -> {
                    Text(
                        text = "불러오는 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 나의 감정 기록 섹션
//        when (uiState) {
//            is HomeUiState.Success -> {
//                (uiState as HomeUiState.Success).weeklyEmotionSummary?.let { summary ->
//                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                        Text(
//                            text = "나의 감정 기록",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold,
//                        )
////                        EmotionRecordCard(emotionSummary = summary)
//                    }
//                }
//            }
//            else -> {}
//        }
        }
    }
}
