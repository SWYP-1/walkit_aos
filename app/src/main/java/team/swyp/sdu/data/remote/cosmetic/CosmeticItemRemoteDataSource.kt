package team.swyp.sdu.data.remote.cosmetic

import team.swyp.sdu.data.api.cosmetic.CosmeticItemApi
import team.swyp.sdu.data.remote.cosmetic.dto.CosmeticItemDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 코스메틱 아이템 데이터 소스
 */
@Singleton
class CosmeticItemRemoteDataSource @Inject constructor(
    private val cosmeticItemApi: CosmeticItemApi,
) {

    /**
     * 코스메틱 아이템 목록 조회
     *
     * @param position 아이템 위치 필터 (HEAD, BODY, FEET). null이면 전체 조회
     * @return 코스메틱 아이템 DTO 목록
     */
    suspend fun getCosmeticItems(position: String? = null): List<CosmeticItemDto> {
        return try {
            val response = cosmeticItemApi.getCosmeticItems(position)

            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                Timber.d("코스메틱 아이템 조회 성공: ${items.size}개${position?.let { " (position: $it)" } ?: ""}")
                items
            } else {
                val errorMessage = response.errorBody()?.string() ?: "코스메틱 아이템 조회 실패"
                Timber.e("코스메틱 아이템 조회 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("코스메틱 아이템 조회 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "코스메틱 아이템 조회 중 예외 발생")
            throw e
        }
    }
}
