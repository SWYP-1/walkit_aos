package team.swyp.sdu.data.local.database

import androidx.room.TypeConverter
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.utils.EnumConverter
import team.swyp.sdu.domain.service.ActivityType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.domain.model.Grade

/**
 * Room Type Converters
 *
 * 복잡한 타입(리스트, enum 등)을 데이터베이스에 저장하기 위한 변환기입니다.
 * 공통 변환 로직은 EnumConverter에 위임합니다.
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
        } catch (e: Throwable) {
            // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
            emptyList()
        }

    @TypeConverter
    fun fromSyncState(value: SyncState?): String = EnumConverter.fromSyncState(value)

    @TypeConverter
    fun toSyncState(value: String?): SyncState = EnumConverter.toSyncState(value)

    @TypeConverter
    fun fromGrade(value: Grade?): String = EnumConverter.fromGrade(value)

    @TypeConverter
    fun toGrade(value: String?): Grade = EnumConverter.toGrade(value)
}
