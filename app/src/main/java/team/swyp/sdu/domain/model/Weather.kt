package team.swyp.sdu.domain.model

import team.swyp.sdu.data.remote.home.dto.PrecipType
import team.swyp.sdu.data.remote.home.dto.Sky

/**
 * 날씨 정보 도메인 모델
 */
data class Weather(
    val nx: Int = 0,
    val ny: Int = 0,
    val generatedAt: String? = null,
    val tempC: Double = 0.0,
    val rain1hMm: Double = 0.0,
    val precipType: PrecipType = PrecipType.NONE,
    val sky: Sky = Sky.SUNNY,
)





