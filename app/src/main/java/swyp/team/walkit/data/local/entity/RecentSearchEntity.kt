package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 스팟 최근 검색어 로컬 저장 엔티티
 *
 * @property query      검색어 (PK)
 * @property searchedAt 검색 시각 (epoch millis, 최신순 정렬에 사용)
 */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val searchedAt: Long,
)
