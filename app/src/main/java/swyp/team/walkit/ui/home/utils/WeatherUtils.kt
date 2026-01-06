package swyp.team.walkit.ui.home.utils

import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.PrecipType
import swyp.team.walkit.domain.model.Sky

/**
 * 날씨 타입 (API 응답용)
 */
@Serializable
enum class WeatherType {
    SUNNY, RAIN, SNOW, OVERCAST
}

/**
 * 날씨 아이콘 타입 (UI 표시용)
 */
enum class WeatherIcon {
    SUNNY, RAINY, SNOW, OVERCAST
}

@DrawableRes
fun resolveWeatherIconRes(weatherType: WeatherType?): Int {
    return when (weatherType) {
        WeatherType.SUNNY -> R.drawable.ic_weather_sunny
        WeatherType.RAIN -> R.drawable.ic_weather_rainy
        WeatherType.SNOW -> R.drawable.ic_weather_snow
        WeatherType.OVERCAST -> R.drawable.ic_weather_cloud
        null -> R.drawable.ic_weather_sunny // 기본값
    }
}
