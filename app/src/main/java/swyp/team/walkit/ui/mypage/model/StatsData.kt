package swyp.team.walkit.ui.mypage.model

/**
 * 통계 데이터
 */
data class StatsData(
    val totalStepCount: Int,
    val totalWalkingTime: Long // 누적 산책 시간 (밀리초)
)