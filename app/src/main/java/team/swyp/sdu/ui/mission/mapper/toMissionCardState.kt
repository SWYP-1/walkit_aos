package team.swyp.sdu.ui.mission.mapper

import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.ui.mission.model.toCardState

/**
 * @deprecated Use WeeklyMission.toCardState() extension function instead
 */
@Deprecated(
    message = "Use WeeklyMission.toCardState() extension function instead",
    replaceWith = ReplaceWith("mission.toCardState(isActive)")
)
fun WeeklyMission.toMissionCardState(
    isActive: Boolean
): team.swyp.sdu.ui.mission.model.MissionCardState {
    return this.toCardState(isActive)
}