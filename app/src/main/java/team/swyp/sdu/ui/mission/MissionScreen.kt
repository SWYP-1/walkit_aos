package team.swyp.sdu.ui.mission

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.mission.component.CategoryChip
import team.swyp.sdu.ui.mission.component.MissionCard
import team.swyp.sdu.ui.mission.component.PopularMissionCard
import team.swyp.sdu.ui.mission.model.MissionCardState
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme


@Composable
fun MissionRoute(
    onNavigateToWalk: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MissionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MissionScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onMissionClick = { missionId ->

        },
        onNavigateToWalk = onNavigateToWalk,
        modifier = modifier,
    )
}

@Composable
fun MissionScreen(
    uiState: MissionUiState,
    onNavigateBack: () -> Unit,
    onNavigateToWalk: () -> Unit,
    onMissionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<MissionCategory?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {

        // 헤더
        AppHeader(
            title = "미션",
            onNavigateBack = onNavigateBack,
        )

        // 프로모션 or 내부 서비스 광고 예정
        Image(
            painter = painterResource(R.drawable.bg_mission_banner),
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            contentDescription = "banner"
        )


        // 메인 콘텐츠
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // 카테고리별 미션 섹션
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "오늘의 미션",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // 필터 칩
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MissionCategory.getFilterCategories().forEach { category ->
                            CategoryChip(
                                category = category,
                                isSelected = selectedCategory == category,
                                onClick = {
                                    selectedCategory =
                                        if (selectedCategory == category) null else category
                                },
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }

            // 미션 리스트 (카테고리 필터링 적용)
            val filteredMissions = if (selectedCategory != null) {
                uiState.weeklyMissions.filter { missionCard ->
                    missionCard.mission.category == selectedCategory
                }
            } else {
                uiState.weeklyMissions
            }

            itemsIndexed(filteredMissions) { index, missionCard ->
                MissionCard(
                    cardState = missionCard.cardState,
                    mission = missionCard.mission,
                    onChallengeClick = onNavigateToWalk,
                    onRewardClick = { missionId -> onMissionClick(missionId) },
                    isActive = index == 0 // 첫 번째 카드(index 0)에만 특별 처리
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
@Preview
fun MissionPreview() {
    WalkItTheme {
        MissionScreen(
            uiState = MissionUiState(),
            onNavigateBack = {},
            onMissionClick = {},
            onNavigateToWalk = {}
        )
    }
}