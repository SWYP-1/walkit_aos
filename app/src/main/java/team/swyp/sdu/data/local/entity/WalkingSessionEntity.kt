package team.swyp.sdu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import team.swyp.sdu.data.local.database.Converters
import team.swyp.sdu.domain.service.ActivityType

/**
 * Room Entity for WalkingSession
 *
 * WalkingSession 도메인 모델을 데이터베이스에 저장하기 위한 Entity입니다.
 * LocationPoint 리스트는 JSON으로 직렬화하여 저장합니다.
 */
@Entity(tableName = "walking_sessions")
@TypeConverters(Converters::class)
data class WalkingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val stepCount: Int = 0,
    val locationsJson: String = "[]", // LocationPoint 리스트를 JSON으로 직렬화
    val totalDistance: Float = 0f,
    val isSynced: Boolean = false, // 서버 동기화 여부
    // 산책 저장 API용 필드들
    val preWalkEmotion: String? = null, // EmotionType을 String으로 저장
    val postWalkEmotion: String? = null, // EmotionType을 String으로 저장
    val note: String? = null,
    val imageUrl: String? = null,
    val createdDate: String? = null,
)
