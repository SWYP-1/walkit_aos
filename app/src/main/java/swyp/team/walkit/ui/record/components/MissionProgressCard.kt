package swyp.team.walkit.ui.record.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.domain.model.MissionProgress
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
internal fun MissionProgressCard(
    progress: MissionProgress,
    modifier: Modifier = Modifier,
    onClaimReward: (Long) -> Unit = {},
) {
    if (progress is MissionProgress.None) return

    val rewardPoints = when (progress) {
        is MissionProgress.Steps -> progress.rewardPoints
        is MissionProgress.Attendance -> progress.rewardPoints
        is MissionProgress.PhotoColor -> progress.rewardPoints
        is MissionProgress.None -> 0
    }
    val isReadyForClaim = when (progress) {
        is MissionProgress.Steps -> progress.isReadyForClaim
        is MissionProgress.Attendance -> progress.isReadyForClaim
        is MissionProgress.PhotoColor -> progress.isReadyForClaim
        is MissionProgress.None -> false
    }
    val userWeeklyMissionId = when (progress) {
        is MissionProgress.Steps -> progress.userWeeklyMissionId
        is MissionProgress.Attendance -> progress.userWeeklyMissionId
        is MissionProgress.PhotoColor -> progress.userWeeklyMissionId
        is MissionProgress.None -> null
    }
    val isCompleted = when (progress) {
        is MissionProgress.Steps -> progress.percentage >= 1f
        is MissionProgress.Attendance -> progress.percentage >= 1f
        is MissionProgress.PhotoColor -> progress.isCompleted
        is MissionProgress.None -> false
    }

    Column(
        modifier = modifier
            .customShadow()
            .cardBorder()
            .background(SemanticColor.backgroundWhitePrimary, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        when (progress) {
            is MissionProgress.Steps -> MissionBarContent(
                percentage = progress.percentage,
                missionName = progress.missionName,
            )

            is MissionProgress.Attendance -> MissionBarContent(
                percentage = progress.percentage,
                missionName = progress.missionName,
            )

            is MissionProgress.PhotoColor -> MissionPhotoColorContent(progress)
            is MissionProgress.None -> {}
        }

        if (rewardPoints > 0) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isCompleted) SemanticColor.buttonPrimaryDefault
                        else SemanticColor.backgroundWhiteTertiary,
                        shape = RoundedCornerShape(size = 8.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .then(
                        if (isReadyForClaim && userWeeklyMissionId != null)
                            Modifier.clickable { onClaimReward(userWeeklyMissionId) }
                        else Modifier
                    ),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${rewardPoints}포인트 받기",
                    style = MaterialTheme.walkItTypography.bodyS.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isCompleted) SemanticColor.buttonPrimaryDefault
                    else SemanticColor.textBorderTertiary,
                )
            }
        }
    }
}

@Composable
private fun MissionBarContent(
    percentage: Float,
    missionName: String,
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 600),
        label = "missionProgressBar",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "미션 달성률",
            style = MaterialTheme.walkItTypography.bodyL.copy(fontWeight = FontWeight.SemiBold),
            color = SemanticColor.backgroundDarkPrimary,
        )
        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MaterialTheme.walkItTypography.bodyL.copy(fontWeight = FontWeight.SemiBold),
            color = SemanticColor.stateGreenPrimary,
        )
    }
    Spacer(Modifier.height(4.dp))
    if (missionName.isNotBlank()) {
        Text(
            text = missionName,
            style = MaterialTheme.walkItTypography.bodyS.copy(fontWeight = FontWeight.Normal),
            color = SemanticColor.textBorderSecondary,
        )
    }

    Spacer(Modifier.height(12.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SemanticColor.backgroundWhiteSecondary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedPercentage)
                .fillMaxHeight()
                .background(SemanticColor.buttonPrimaryDefault),
        )
    }
}

@Composable
private fun MissionPhotoColorContent(progress: MissionProgress.PhotoColor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "미션 달성률",
            style = MaterialTheme.walkItTypography.bodyL.copy(fontWeight = FontWeight.SemiBold),
            color = SemanticColor.backgroundDarkPrimary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (progress.isCompleted) SemanticColor.stateGreenPrimary.copy(alpha = 0.12f)
                    else SemanticColor.backgroundWhiteSecondary
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (progress.isCompleted) "달성" else "미달성",
                style = MaterialTheme.walkItTypography.captionM.copy(fontWeight = FontWeight.SemiBold),
                color = if (progress.isCompleted) SemanticColor.stateGreenPrimary
                else SemanticColor.textBorderSecondary,
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = "${progress.targetColor} 색상 사진 촬영",
        style = MaterialTheme.walkItTypography.bodyS,
        color = SemanticColor.textBorderSecondary,
    )
}

@Preview(showBackground = true, name = "걸음 수 — 진행 중 (6%)")
@Composable
private fun MissionProgressStepsInProgressPreview() {
    WalkItTheme {
        MissionProgressCard(
            progress = MissionProgress.Steps(current = 1_300, target = 20_000, rewardPoints = 200),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "걸음 수 — 달성 (100%)")
@Composable
private fun MissionProgressStepsAchievedPreview() {
    WalkItTheme {
        MissionProgressCard(
            progress = MissionProgress.Steps(current = 20_000, target = 20_000, rewardPoints = 200),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "출석 — 2/3일")
@Composable
private fun MissionProgressAttendancePreview() {
    WalkItTheme {
        MissionProgressCard(
            progress = MissionProgress.Attendance(current = 2, target = 3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "사진 색상 — 미달성")
@Composable
private fun MissionProgressPhotoColorNotDonePreview() {
    WalkItTheme {
        MissionProgressCard(
            progress = MissionProgress.PhotoColor(targetColor = "빨간색", isCompleted = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "사진 색상 — 달성")
@Composable
private fun MissionProgressPhotoColorDonePreview() {
    WalkItTheme {
        MissionProgressCard(
            progress = MissionProgress.PhotoColor(targetColor = "빨간색", isCompleted = true),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
