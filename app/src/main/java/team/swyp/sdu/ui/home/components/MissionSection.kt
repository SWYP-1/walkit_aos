package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.ui.home.MissionUiState
import team.swyp.sdu.ui.home.MissionWithState
import team.swyp.sdu.ui.mission.component.MissionCard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.shimmer
import timber.log.Timber

/**
 * 미션 섹션 (오늘의 추천 미션)
 */
@Composable
fun MissionSection(
    uiState: MissionUiState,
    onClickMission: () -> Unit,
    onRewardClick: (Long) -> Unit,
    onClickMissionMore: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier.fillMaxWidth()) {
        // 섹션 헤더 (항상 표시)
        MissionSectionHeader(onClickMissionMore = onClickMissionMore)

        // 섹션 콘텐츠
        when (uiState) {
            is MissionUiState.Loading -> {
                Timber.d("MissionSkeleton 표시")
                MissionSkeleton()
            }

            is MissionUiState.Success -> {
                Timber.d("MissionContent 표시")
                MissionContent(uiState, onClickMission,onRewardClick)
            }

            is MissionUiState.Empty -> {
                Timber.d("MissionEmpty 표시")
                MissionEmpty()
            }

            is MissionUiState.Error -> {
                Timber.d("MissionError 표시: ${uiState.message}")
                MissionEmpty()
            }
        }
    }
}

/**
 * 미션 섹션 헤더
 */
@Composable
private fun MissionSectionHeader(
    modifier: Modifier = Modifier,
    onClickMissionMore: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "오늘의 추천 미션",
            modifier = modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))  // 1️⃣ 클릭 영역 자르기
                .clickable(onClick = onClickMissionMore)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Text(
                text = "더보기",
                // body M/medium
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "더보기", tint = SemanticColor.iconGrey
            )
        }
    }

}

/**
 * 미션 콘텐츠 (성공 상태)
 */
@Composable
private fun MissionContent(
    uiState: MissionUiState.Success,
    onClickMission: () -> Unit,
    onRewardClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Timber.d("MissionContent 렌더링 - missionCardStates 크기: ${uiState.missionCardStates.size}")

    if (uiState.missionCardStates.isEmpty()) {
        Timber.d("MissionEmpty 표시")
        MissionEmpty()
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.missionCardStates.forEach { missionWithState ->
                Timber.d("미션 카드 렌더링: ${missionWithState.mission.title}, 상태: ${missionWithState.cardState}")
                MissionCard(
                    mission = missionWithState.mission,
                    cardState = missionWithState.cardState,
                    onChallengeClick = onClickMission,
                    onRewardClick = onRewardClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 미션 스켈레톤 (레이아웃 계약 준수)
 * - 최종 Content와 1:1 레이아웃 구조 유지
 * - padding / spacing / shape / weight 동일
 * - height를 직접 지정하지 않음 (영역 고정)
 */
@Composable
private fun MissionSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 미션 카드 스켈레톤들 (최대 2개 표시)
        repeat(2) {
            MissionCardSkeleton()
        }
    }
}

/**
 * 미션 카드 스켈레톤
 * - Card 단위로 shimmer 적용 (카드 전체에 shimmer)
 */
@Composable
private fun MissionCardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // 카드 높이 고정 (레이아웃 계약)
            .background(
                color = SemanticColor.backgroundWhiteSecondary,
                shape = RoundedCornerShape(16.dp)
            )
            .shimmer() // Shimmer 적용: 카드 단위
    )
}


/**
 * 미션 빈 상태
 */
@Composable
private fun MissionEmpty(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp), // 영역 유지 (레이아웃 계약)
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "오늘의 미션이 없습니다",
            style = MaterialTheme.walkItTypography.bodyM,
            color = SemanticColor.textBorderSecondary
        )
    }
}

/**
 * 미션 에러 상태 (재시도 포함, 영역 유지)
 */
@Composable
private fun MissionError(
    message: String,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp), // 영역 유지 (레이아웃 계약)
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.walkItTypography.bodyM,
                color = SemanticColor.textBorderPrimary
            )

            // 재시도 버튼
            Text(
                text = "다시 시도",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.buttonPrimaryDefault,
                modifier = Modifier
                    .background(
                        color = SemanticColor.buttonPrimaryDisabled,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                // .clickable(onClick = onRetry) // TODO: 재시도 로직 연결
            )
        }
    }
}
