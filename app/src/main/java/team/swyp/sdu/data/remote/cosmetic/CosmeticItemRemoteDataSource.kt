package team.swyp.sdu.data.remote.cosmetic

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.api.cosmetic.CosmeticItemApi
import team.swyp.sdu.data.remote.cosmetic.dto.CosmeticItemDto
import team.swyp.sdu.data.remote.cosmetic.dto.PurchaseRequestDto
import team.swyp.sdu.data.remote.cosmetic.dto.PurchaseResponseDto
import team.swyp.sdu.data.remote.cosmetic.dto.WearItemRequest
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
            Timber.d("코스메틱 아이템 API 호출 시작: position=$position")
            val response = cosmeticItemApi.getCosmeticItems(position)
            Timber.d("API 응답 수신: isSuccessful=${response.isSuccessful}, code=${response.code()}")

            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                Timber.d("코스메틱 아이템 조회 성공: ${items.size}개${position?.let { " (position: $it)" } ?: ""}")

                if (items.isNotEmpty()) {
                    Timber.d("첫 번째 아이템 샘플: itemId=${items[0].itemId}, name=${items[0].name}, position=${items[0].position}")
                }

                items
            } else {
                val errorMessage = response.errorBody()?.string() ?: "코스메틱 아이템 조회 실패"
                Timber.e("코스메틱 아이템 조회 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("코스메틱 아이템 조회 실패: ${response.code()}")
            }
        } catch (t: Throwable) {
            Timber.e(t, "코스메틱 아이템 조회 중 예외 발생: ${t.message}")
            throw t
        }
    }

    /**
     * 코스메틱 아이템 구매
     *
     * @param request 구매 요청 데이터
     * @return 구매 결과 (성공 시 Unit)
     */
    suspend fun purchaseItems(request: PurchaseRequestDto): Result<Unit> {
        return try {
            val response = cosmeticItemApi.purchaseItems(request)

            if (response.isSuccessful) {
                Timber.d("코스메틱 아이템 구매 성공 (HTTP ${response.code()})")
                Result.Success(Unit)
            } else {
                val errorMessage = getPurchaseErrorMessage(response.code())
                Timber.e("코스메틱 아이템 구매 실패: $errorMessage (코드: ${response.code()})")
                Result.Error(Exception(errorMessage))
            }
        } catch (t: Throwable) {
            Timber.e(t, "코스메틱 아이템 구매 중 예외 발생 (Ask Gemini)")
            Result.Error(t, "코스메틱 아이템 구매 중 예외 발생")
        }
    }

    /**
     * 구매 API 에러 코드에 따른 에러 메시지 반환
     *
     * @param statusCode HTTP 상태 코드
     * @return 사용자 친화적인 에러 메시지
     */
    private fun getPurchaseErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            404 -> "아이템을 찾을 수 없습니다"
            400 -> "아이템을 구매할 포인트가 부족합니다"
            409 -> "이미 구매한 아이템입니다"
            else -> "구매 처리 중 오류가 발생했습니다 (코드: $statusCode)"
        }
    }

    /**
     * 캐릭터 아이템 착용/해제
     *
     * @param itemId 아이템 ID
     * @param isWorn 착용 여부 (true: 착용, false: 해제)
     * @return 착용/해제 결과
     */
    suspend fun wearItem(itemId: Int, isWorn: Boolean): Result<Unit> {
        return try {
            val request = WearItemRequest(isWorn = isWorn)
            val response = cosmeticItemApi.wearItem(itemId, request)

            if (response.isSuccessful) {
                Timber.d("캐릭터 아이템 ${if (isWorn) "착용" else "해제"} 성공: itemId=$itemId")
                Result.Success(Unit)
            } else {
                val errorMessage = getWearItemErrorMessage(response.code())
                Timber.e("캐릭터 아이템 ${if (isWorn) "착용" else "해제"} 실패: $errorMessage (코드: ${response.code()})")
                Result.Error(Exception(errorMessage))
            }
        } catch (t: Throwable) {
            Timber.e(t, "캐릭터 아이템 착용/해제 중 예외 발생: itemId=$itemId")
            Result.Error(t)
        }
    }

    /**
     * 착용/해제 API 에러 코드에 따른 에러 메시지 반환
     *
     * @param statusCode HTTP 상태 코드
     * @return 사용자 친화적인 에러 메시지
     */
    private fun getWearItemErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            403 -> "구매하지 않은 아이템은 착용할 수 없습니다"
            else -> "아이템 착용 처리 중 오류가 발생했습니다 (코드: $statusCode)"
        }
    }
}

