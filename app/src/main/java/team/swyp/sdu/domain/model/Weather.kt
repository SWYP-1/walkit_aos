package team.swyp.sdu.domain.model

/**
 * 강수 형태 (도메인 모델)
 */
enum class PrecipType(val displayName: String) {
    NONE("없음"),
    RAIN("비"),
    RAIN_SNOW("눈비"),
    SNOW("눈"),
    SHOWER("소나기"),
    UNKNOWN("알 수 없음");

    companion object {
        /**
         * API DTO PrecipType을 도메인 PrecipType으로 변환
         */
        fun fromApiPrecipType(apiPrecipType: team.swyp.sdu.data.remote.home.dto.PrecipType): PrecipType {
            return when (apiPrecipType) {
                team.swyp.sdu.data.remote.home.dto.PrecipType.NONE -> NONE
                team.swyp.sdu.data.remote.home.dto.PrecipType.RAIN -> RAIN
                team.swyp.sdu.data.remote.home.dto.PrecipType.RAIN_SNOW -> RAIN_SNOW
                team.swyp.sdu.data.remote.home.dto.PrecipType.SNOW -> SNOW
                team.swyp.sdu.data.remote.home.dto.PrecipType.SHOWER -> SHOWER
                team.swyp.sdu.data.remote.home.dto.PrecipType.UNKNOWN -> UNKNOWN
            }
        }
    }
}

/**
 * 하늘 상태 (도메인 모델)
 */
enum class Sky(val displayName: String) {
    SUNNY("맑음"),
    CLOUDY_MANY("구름많음"),
    OVERCAST("흐림"),
    UNKNOWN("알 수 없음");

    companion object {
        /**
         * API DTO Sky를 도메인 Sky로 변환
         */
        fun fromApiSky(apiSky: team.swyp.sdu.data.remote.home.dto.Sky): Sky {
            return when (apiSky) {
                team.swyp.sdu.data.remote.home.dto.Sky.SUNNY -> SUNNY
                team.swyp.sdu.data.remote.home.dto.Sky.CLOUDY_MANY -> CLOUDY_MANY
                team.swyp.sdu.data.remote.home.dto.Sky.OVERCAST -> OVERCAST
                team.swyp.sdu.data.remote.home.dto.Sky.UNKNOWN -> UNKNOWN
            }
        }
    }
}

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






