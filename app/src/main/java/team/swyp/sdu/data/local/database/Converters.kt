package team.swyp.sdu.data.local.database

import androidx.room.TypeConverter
import team.swyp.sdu.data.model.ActivityStats
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.domain.service.ActivityType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room Type Converters
 *
 * 복잡한 타입(리스트, enum 등)을 데이터베이스에 저장하기 위한 변환기입니다.
 */
class Converters {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @TypeConverter
    fun fromLocationPointList(value: List<LocationPoint>): String = json.encodeToString(value)

    @TypeConverter
    fun toLocationPointList(value: String): List<LocationPoint> =
        try {
            json.decodeFromString<List<LocationPoint>>(value)
        } catch (e: Exception) {
            emptyList()
        }

    @TypeConverter
    fun fromActivityStatsList(value: List<ActivityStats>): String = json.encodeToString(value)

    @TypeConverter
    fun toActivityStatsList(value: String): List<ActivityStats> =
        try {
            json.decodeFromString<List<ActivityStats>>(value)
        } catch (e: Exception) {
            emptyList()
        }

    @TypeConverter
    fun fromActivityType(value: ActivityType?): String? = value?.name

    @TypeConverter
    fun toActivityType(value: String?): ActivityType? =
        value?.let {
            try {
                ActivityType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
}
