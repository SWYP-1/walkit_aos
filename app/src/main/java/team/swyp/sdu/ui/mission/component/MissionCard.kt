package team.swyp.sdu.ui.mission.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.domain.model.MissionStatus
import team.swyp.sdu.domain.model.MissionType
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.ui.home.components.clickableNoRipple
import team.swyp.sdu.ui.mission.model.MissionCardState
import team.swyp.sdu.ui.record.components.customShadow
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography


/* =====================================================
 * 3. UI 표현 매핑
 * ===================================================== */

private fun MissionCardState.buttonText(): String =
    when (this) {
        MissionCardState.INACTIVE,
        MissionCardState.ACTIVE_CHALLENGE -> "도전 하기"

        MissionCardState.READY_FOR_CLAIM -> "보상 받기"
        MissionCardState.COMPLETED -> "보상 완료"
    }

private fun MissionCardState.buttonTextColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.textBorderSecondary
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.textBorderPrimaryInverse
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.buttonPrimaryDefault
        MissionCardState.COMPLETED -> SemanticColor.textBorderPrimaryInverse
    }

private fun MissionCardState.backgroundColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.backgroundWhiteSecondary
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.backgroundWhitePrimary
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.backgroundWhitePrimary
        MissionCardState.COMPLETED -> SemanticColor.backgroundWhitePrimary
    }

private fun MissionCardState.buttonBackgroundColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.buttonPrimaryDisabled
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.textBorderPrimary
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.backgroundWhitePrimary
        MissionCardState.COMPLETED -> SemanticColor.textBorderPrimary
    }

private fun MissionCardState.categoryTextColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.textBorderSecondary
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.stateBluePrimary
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.stateBluePrimary
        MissionCardState.COMPLETED -> SemanticColor.stateBluePrimary
    }

private fun MissionCardState.categoryBackgroundColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.backgroundWhiteTertiary
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.stateBlueTertiary
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.stateBlueTertiary
        MissionCardState.COMPLETED -> SemanticColor.stateBlueTertiary
    }


private fun MissionCardState.rewardTextColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.textBorderTertiary
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.textBorderGreenSecondary
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.textBorderGreenSecondary
        MissionCardState.COMPLETED -> SemanticColor.textBorderGreenSecondary
    }

private fun MissionCardState.buttonBorder(): BorderStroke? =
    when (this) {
        MissionCardState.INACTIVE -> null
        MissionCardState.ACTIVE_CHALLENGE -> null
        MissionCardState.READY_FOR_CLAIM -> BorderStroke(
            width = 1.dp,
            color = SemanticColor.textBorderGreenPrimary
        )

        MissionCardState.COMPLETED -> BorderStroke(
            width = 1.dp,
            color = SemanticColor.buttonPrimaryDisabled
        )
    }


private fun MissionCardState.titleTextColor(): Color =
    when (this) {
        MissionCardState.INACTIVE -> SemanticColor.textBorderSecondary
        MissionCardState.ACTIVE_CHALLENGE -> SemanticColor.textBorderPrimary
        MissionCardState.READY_FOR_CLAIM -> SemanticColor.textBorderPrimary
        MissionCardState.COMPLETED -> SemanticColor.textBorderPrimary
    }

private val MissionCardShape = RoundedCornerShape(8.dp)

fun Modifier.missionCardBorder(
    state: MissionCardState,
    shape: Shape = MissionCardShape
): Modifier {
    val border = state.buttonBorder()
    return if (border != null) {
        this
            .border(border, shape)
    } else {
        this
    }
}



/* =====================================================
 * 4. MissionCard
 * ===================================================== */

@Composable
fun MissionCard(
    mission: WeeklyMission,
    cardState: MissionCardState,
    onChallengeClick: () -> Unit,
    onRewardClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .customShadow(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardState.backgroundColor()
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Row() {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardState.categoryBackgroundColor())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = mission.category.displayName,
                            style = MaterialTheme.walkItTypography.captionM.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = cardState.categoryTextColor()
                        )
                    }
                }
                Text(
                    text = mission.title,
                    // body L/semibold
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = cardState.titleTextColor()
                )
                Text(
                    text = "${mission.rewardPoints} Exp",
                    // body S/medium
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = cardState.rewardTextColor()
                )
            }

            val buttonModifier = when (cardState) {
                MissionCardState.READY_FOR_CLAIM ->
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onRewardClick(mission.userWeeklyMissionId ?: 0L)
                    }

                MissionCardState.ACTIVE_CHALLENGE ->
                    Modifier.clickableNoRipple(
                        onClick = onChallengeClick
                    )

                else ->
                    Modifier // 아무 Modifier도 안 붙음 = 클릭 불가
            }
            Box(
                modifier = Modifier
                    .then(buttonModifier)
                    .missionCardBorder(cardState)
                    .background(
                        color = cardState.buttonBackgroundColor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(MissionCardShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = cardState.buttonText(),
                    color = cardState.buttonTextColor(),
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/* =====================================================
 * 5. Preview
 * ===================================================== */

private fun previewMission(status: MissionStatus) = WeeklyMission(
    userWeeklyMissionId = 1L,
    missionId = 100L,
    title = "주간 5만보 걷기",
    description = "이번 주 목표 걸음수를 달성하세요",
    category = MissionCategory.CHALLENGE,
    type = MissionType.CHALLENGE_STEPS,
    status = status,
    rewardPoints = 500,
    assignedConfigJson = "{}",
    weekStart = "2025-01-01",
    weekEnd = "2025-01-07"
)

@Preview(showBackground = true, name = "ACTIVE_CHALLENGE")
@Composable
fun PreviewActiveChallenge() {
    WalkItTheme {
        MissionCard(
            mission = previewMission(MissionStatus.IN_PROGRESS),
            cardState = MissionCardState.ACTIVE_CHALLENGE,
            onChallengeClick = {},
            onRewardClick = {}
        )
    }
}

@Preview(showBackground = true, name = "COMPLETED")
@Composable
fun PreviewCompleted() {
    WalkItTheme {
        MissionCard(
            mission = previewMission(MissionStatus.COMPLETED),
            cardState = MissionCardState.READY_FOR_CLAIM,
            onChallengeClick = {},
            onRewardClick = {}
        )
    }
}

@Preview(showBackground = true, name = "FAILED")
@Composable
fun PreviewFailed() {
    WalkItTheme {
        MissionCard(
            mission = previewMission(MissionStatus.FAILED),
            cardState = MissionCardState.INACTIVE,
            onChallengeClick = {},
            onRewardClick = {}
        )
    }
}

@Preview(showBackground = true, name = "INACTIVE")
@Composable
fun PreviewInactive() {
    WalkItTheme {
        MissionCard(
            mission = previewMission(MissionStatus.COMPLETED),
            cardState = MissionCardState.INACTIVE,
            onChallengeClick = {},
            onRewardClick = {}
        )
    }
}


@Preview(showBackground = true, name = "READY_FOR_CLAIM")
@Composable
fun PreviewReadyToClaim() {
    WalkItTheme {
        MissionCard(
            mission = previewMission(MissionStatus.COMPLETED),
            cardState = MissionCardState.READY_FOR_CLAIM,
            onChallengeClick = {},
            onRewardClick = {}
        )
    }
}
