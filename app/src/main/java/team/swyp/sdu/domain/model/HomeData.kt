package team.swyp.sdu.domain.model

/**
 * 홈 화면 데이터 도메인 모델
 */
data class HomeData(
    val character: Character,
    val walkProgressPercentage: String,
    val todaySteps: Int,
    val weather: Weather? = null,
    val weeklyMission: WeeklyMission? = null,
    val walkRecords: List<WalkRecord> = emptyList(),
)



