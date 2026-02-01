package swyp.team.walkit.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import swyp.team.walkit.core.Result
import swyp.team.walkit.core.Result.*
import swyp.team.walkit.data.remote.cosmetic.CosmeticItemRemoteDataSource
import swyp.team.walkit.data.remote.cosmetic.dto.PurchaseItemDto
import swyp.team.walkit.data.remote.cosmetic.dto.PurchaseRequestDto
import swyp.team.walkit.data.remote.cosmetic.mapper.CosmeticItemMapper
import swyp.team.walkit.domain.model.CosmeticItem
import swyp.team.walkit.domain.repository.CosmeticItemRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import retrofit2.HttpException
import java.io.IOException
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
            Timber.d("코스메틱 아이템 조회 시작${position?.let { " (position: $it)" } ?: ""}")
            val dtos = cosmeticItemRemoteDataSource.getCosmeticItems(position)
            Timber.d("RemoteDataSource에서 받은 DTO 개수: ${dtos.size}")

            if (dtos.isNotEmpty()) {
                Timber.d("첫 번째 아이템 샘플: ${dtos[0]}")
            }

            val domainItems = CosmeticItemMapper.toDomainList(dtos)
            Timber.d("코스메틱 아이템 조회 완료: ${domainItems.size}개${position?.let { " (position: $it)" } ?: ""}")
            Result.Success(data = domainItems)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "getCosmeticItems")
            Timber.e(e, "코스메틱 아이템 조회 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "getCosmeticItems")
            Timber.e(e, "코스메틱 아이템 조회 실패: HTTP ${e.code()}")
            Result.Error(e, "코스메틱 아이템을 불러올 수 없습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }

    override suspend fun purchaseItems(
        items: List<CosmeticItem>,
        totalPrice: Int
    ): Result<Unit> {
        return try {
            val request = PurchaseRequestDto(
                items = items.map { PurchaseItemDto(it.itemId) },
                totalPrice = totalPrice
            )

            cosmeticItemRemoteDataSource.purchaseItems(request)
            Result.Success(Unit)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "purchaseItems")
            Timber.e(e, "코스메틱 아이템 구매 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "purchaseItems")
            Timber.e(e, "코스메틱 아이템 구매 실패: HTTP ${e.code()}")
            Result.Error(e, "아이템 구매에 실패했습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }


    override suspend fun wearItem(itemId: Int, isWorn: Boolean): Result<Unit> {
        return try {
            cosmeticItemRemoteDataSource.wearItem(itemId, isWorn)
            Result.Success(Unit)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "wearItem")
            Timber.e(e, "코스메틱 아이템 착용/해제 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "wearItem")
            Timber.e(e, "코스메틱 아이템 착용/해제 실패: HTTP ${e.code()}")
            Result.Error(e, "아이템 착용/해제에 실패했습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }
}









