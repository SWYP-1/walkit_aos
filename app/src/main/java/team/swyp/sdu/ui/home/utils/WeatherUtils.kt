package team.swyp.sdu.ui.home.utils

import androidx.annotation.DrawableRes
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.PrecipType
import team.swyp.sdu.domain.model.Sky

/**
 * 날씨 아이콘 타입
 */
enum class WeatherIcon {
    SUNNY,
    CLOUDY,
    RAINY,
    SNOW
}

@DrawableRes
fun resolveWeatherIconRes(
    precipType: PrecipType,
    sky: Sky
): Int {
    return when (resolveWeatherIcon(precipType, sky)) {
        WeatherIcon.SUNNY -> R.drawable.ic_weather_sunny
        WeatherIcon.CLOUDY -> R.drawable.ic_weather_cloud
        WeatherIcon.RAINY -> R.drawable.ic_weather_rainy
        WeatherIcon.SNOW -> R.drawable.ic_weather_snow
    }
}


/**
 * 강수 형태와 하늘 상태를 기반으로 날씨 아이콘을 결정합니다.
 *
 * @param precipType 강수 형태
 * @param sky 하늘 상태
 * @return 결정된 날씨 아이콘
 */
fun resolveWeatherIcon(
    precipType: PrecipType,
    sky: Sky
): WeatherIcon {
    return when (precipType) {
        PrecipType.SNOW, PrecipType.RAIN_SNOW -> WeatherIcon.SNOW
        PrecipType.RAIN, PrecipType.SHOWER -> WeatherIcon.RAINY
        PrecipType.NONE, PrecipType.UNKNOWN -> {
            when (sky) {
                Sky.SUNNY -> WeatherIcon.SUNNY
                Sky.CLOUDY_MANY, Sky.OVERCAST -> WeatherIcon.CLOUDY
                Sky.UNKNOWN -> WeatherIcon.CLOUDY
            }
        }
    }
}

/**
 * 강수 형태와 하늘 상태를 기반으로 날씨 아이콘을 결정합니다. (String 버전)
 *
 * @param precipitation 강수 형태 문자열
 * @param sky 하늘 상태 문자열
 * @return 결정된 날씨 아이콘
 */
fun resolveWeatherIcon(
    precipitation: String,
    sky: String
): WeatherIcon {
    return when (precipitation) {
        "SNOW", "RAIN_SNOW" -> WeatherIcon.SNOW
        "RAIN", "SHOWER" -> WeatherIcon.RAINY
        else -> {
            when (sky) {
                "SUNNY" -> WeatherIcon.SUNNY
                "CLOUDY_MANY", "OVERCAST" -> WeatherIcon.CLOUDY
                else -> WeatherIcon.CLOUDY
            }
        }
    }
}

