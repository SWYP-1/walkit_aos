package team.swyp.sdu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.cosmetic.CosmeticItemRemoteDataSource
import team.swyp.sdu.data.remote.cosmetic.mapper.CosmeticItemMapper
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.repository.CosmeticItemRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐릭터 꾸미기 아이템 Repository 구현
 */
@Singleton
class CosmeticItemRepositoryImpl @Inject constructor(
    private val cosmeticItemRemoteDataSource: CosmeticItemRemoteDataSource,
) : CosmeticItemRepository {

    override fun getAvailableItems(): Flow<List<CosmeticItem>> {
        // TODO: 로컬 데이터베이스에서 구매 가능한 아이템 조회 로직 구현
        // 현재는 빈 리스트 반환
        return flowOf(emptyList())
    }

    override suspend fun getCosmeticItems(position: String?): Result<List<CosmeticItem>> {
        return try {
            val dtos = cosmeticItemRemoteDataSource.getCosmeticItems(position)
            val domainItems = CosmeticItemMapper.toDomainList(dtos)
            Timber.d("코스메틱 아이템 조회 완료: ${domainItems.size}개${position?.let { " (position: $it)" } ?: ""}")
            Result.Success(data = domainItems)
        } catch (e: Exception) {
            Timber.e(e, "코스메틱 아이템 조회 실패")
            Result.Error(e)
        }
    }
}









