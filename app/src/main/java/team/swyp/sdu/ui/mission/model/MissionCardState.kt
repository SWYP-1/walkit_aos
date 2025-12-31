package team.swyp.sdu.ui.mission.model

import team.swyp.sdu.domain.model.MissionStatus
import team.swyp.sdu.domain.model.WeeklyMission


enum class MissionCardState {
    INACTIVE,          // 대표 미션 아님 (조건 충족해도 리워드 불가)
    ACTIVE_CHALLENGE,  // 대표 미션 + 진행 중
    READY_FOR_CLAIM,   // 미션 완료 (보상 청구 가능)
    COMPLETED          // 미션 완료 (보상까지 완료)
}
