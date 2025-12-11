package team.swyp.sdu.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

/**
 * 홈 화면 (요약/캐릭터/미션/주간 기록)
 */
@Composable
fun HomeScreen(
    onClickWalk: () -> Unit,
    onClickGoal: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val nickname = "닉네임"
    val levelLabel = "새싹 Lv.3"
    val todaySteps = 8_312
    val goalTitle = "목표명 / 달성률"
    val progressRatio = 0.68f
    val missions =
        listOf(
            HomeMission("5,000보 이상 걷기", "300 Exp"),
            HomeMission("5,000보 이상 걷기", "300 Exp"),
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TopPill(text = "로고, 네임")
            TopPill(text = "날씨 / 온도")
        }

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

        team.swyp.sdu.ui.home.components.CharacterSection(
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
    }
}

