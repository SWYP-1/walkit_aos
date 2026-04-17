package swyp.team.walkit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import swyp.team.walkit.data.local.dao.RecentSearchDao
import swyp.team.walkit.data.local.entity.RecentSearchEntity
import swyp.team.walkit.domain.repository.RecentSearchRepository
import javax.inject.Inject

private const val MAX_RECENT_SEARCHES = 5

/**
 * [RecentSearchRepository] 구현체
 *
 * Room을 통해 최근 검색어를 영속적으로 관리한다.
 * 저장 시 중복 제거 + 최대 10개 유지 정책을 적용한다.
 */
class RecentSearchRepositoryImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao,
) : RecentSearchRepository {

    override fun getRecentSearches(): Flow<List<String>> =
        recentSearchDao.getRecentSearches(MAX_RECENT_SEARCHES)
            .map { list -> list.map { it.query } }

    override suspend fun saveSearch(query: String) {
        recentSearchDao.upsert(
            RecentSearchEntity(
                query = query,
                searchedAt = System.currentTimeMillis(),
            )
        )
        recentSearchDao.trimToLimit(MAX_RECENT_SEARCHES)
    }

    override suspend fun deleteSearch(query: String) {
        recentSearchDao.delete(query)
    }

    override suspend fun clearAll() {
        recentSearchDao.deleteAll()
    }
}
