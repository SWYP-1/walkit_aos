package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import swyp.team.walkit.data.local.database.Converters
import swyp.team.walkit.domain.service.ActivityType

/**
 * Room Entity for WalkingSession
 *
 * WalkingSession 도메인 모델을 데이터베이스에 저장하기 위한 Entity입니다.
 * LocationPoint 리스트는 JSON으로 직렬화하여 저장합니다.
 */
@Entity(tableName = "walking_sessions")
@TypeConverters(Converters::class)
data class WalkingSessionEntity(
    @PrimaryKey
    val id: String,
    val userId: Long, // 사용자 ID 추가
    val serverId: String? = null, // 서버에서 받은 세션 ID (중복 저장 방지용)
    val startTime: Long,
    val endTime: Long,
    val stepCount: Int = 0,
    val locationsJson: String = "[]", // LocationPoint 리스트를 JSON으로 직렬화 (원본 데이터)
    val filteredLocationsJson: String? = null, // 필터링된 LocationPoint 리스트 (nullable)
    val smoothedLocationsJson: String? = null, // 스무딩된 LocationPoint 리스트 (nullable)
    val totalDistance: Float = 0f,
    val isSynced: Boolean = false, // 서버 동기화 여부
    // 산책 저장 API용 필드들
    val preWalkEmotion: String, // EmotionType을 String으로 저장
    val postWalkEmotion: String, // EmotionType을 String으로 저장
    val note: String? = null,
    val localImagePath: String? = null, // 로컬 파일 경로
    val serverImageUrl: String? = null, // 서버 URL
    val createdDate: String,
    val syncState: SyncState,
    val targetStepCount: Int = 0, // 산책 당시 설정된 목표 걸음 수
    val targetWalkCount: Int = 0, // 산책 당시 설정된 목표 산책 횟수
)
