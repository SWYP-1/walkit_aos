package swyp.team.walkit.domain.model

import swyp.team.walkit.ui.home.utils.WeatherType

/**
 * 홈 화면 데이터 도메인 모델
 */
data class HomeData(
    val character: Character,
    val walkProgressPercentage: String,
    val todaySteps: Int,
    val temperature: Double? = null,
    val weather: WeatherType? = null,
    val weeklyMission: WeeklyMission? = null,
    val walkRecords: List<WalkRecord> = emptyList(),
)







