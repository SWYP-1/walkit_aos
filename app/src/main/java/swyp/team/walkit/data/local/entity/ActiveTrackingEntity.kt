package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** 필터링된 GPS 좌표 직렬화용 */
@Serializable
data class FilteredPoint(val lat: Double, val lng: Double)

/**
 * 진행 중인 산책 세션의 GPS 포인트를 저장하는 임시 테이블
 *
 * 활성 세션은 항상 하나이므로 id = 1 고정.
 * 정상 종료 시 삭제, OS 강제 킬 시 복구에 사용.
 */
@Entity(tableName = "active_tracking")
data class ActiveTrackingEntity(
    @PrimaryKey
    val id: Int = 1,
    val startTime: Long,
    val locationsJson: String = "[]",
    val filteredLocationsJson: String = "[]",
    val lastFlushTime: Long = 0L,
)
