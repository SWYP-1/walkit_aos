package swyp.team.walkit.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 최근 검색어 도메인 저장소
 */
interface RecentSearchRepository {
    /** 최신순 최근 검색어 Flow */
    fun getRecentSearches(): Flow<List<String>>

    /** 검색어 저장 (최대 10개 유지) */
    suspend fun saveSearch(query: String)

    /** 특정 검색어 삭제 */
    suspend fun deleteSearch(query: String)

    /** 전체 삭제 */
    suspend fun clearAll()
}
