package swyp.team.walkit.ui.mission.model

import swyp.team.walkit.domain.model.MissionStatus
import swyp.team.walkit.domain.model.WeeklyMission


enum class MissionCardState {
    INACTIVE,          // 대표 미션 아님 (조건 충족해도 리워드 불가)
    ACTIVE_CHALLENGE,  // 대표 미션 + 진행 중
    READY_FOR_CLAIM,   // 미션 완료 (보상 청구 가능)
    COMPLETED          // 미션 완료 (보상까지 완료)
}
