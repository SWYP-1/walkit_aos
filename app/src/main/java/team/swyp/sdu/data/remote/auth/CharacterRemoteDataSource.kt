package team.swyp.sdu.data.remote.auth

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.api.auth.CharacterApi
import team.swyp.sdu.data.remote.walking.dto.CharacterDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐릭터 정보 데이터 소스
 */
@Singleton
class CharacterRemoteDataSource @Inject constructor(
    private val characterApi: CharacterApi,
) {


    /**
     * 위치 기반 캐릭터 정보 조회
     *
     * @param lat 위도
     * @param lon 경도
     * @return 캐릭터 정보 DTO
     */
    suspend fun getCharacterByLocation(lat: Double, lon: Double): CharacterDto {
        return try {
            val response = characterApi.getCharacterByLocation(lat, lon)

            if (response.isSuccessful) {
                val characterDto = response.body()
                if (characterDto != null) {
                    Timber.d("위치 기반 캐릭터 정보 조회 성공: lat=$lat, lon=$lon, nickname=${characterDto.nickName}")
                    characterDto
                } else {
                    Timber.e("캐릭터 정보 응답이 null입니다")
                    throw Exception("캐릭터 정보 응답이 null입니다")
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "캐릭터 정보 조회 실패"
                Timber.e("위치 기반 캐릭터 정보 조회 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("캐릭터 정보 조회 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "위치 기반 캐릭터 정보 조회 중 예외 발생")
            throw e
        }
    }
}
