package swyp.team.walkit.domain.repository

import kotlinx.coroutines.flow.Flow
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.Character

/**
 * 캐릭터 정보 Repository 인터페이스
 *
 * Home API에서 받은 Character 정보를 Room에 저장하고 조회합니다.
 * userId를 primary key로 사용하여 User와 1:1 관계를 가집니다.
 */
interface CharacterRepository {
    /**
     * userId로 캐릭터 정보 조회 (Flow)
     */
    fun observeCharacter(userId: Long): Flow<Character?>

    /**
     * userId로 캐릭터 정보 조회 (DB 우선, 없으면 API 호출)
     */
    suspend fun getCharacter(userId: Long): Result<Character>

    /**
     * userId로 캐릭터 정보 조회 (DB만, API 호출 없음)
     */
    suspend fun getCharacterFromDb(userId: Long): Character?

    /**
     * 캐릭터 정보 조회 (API 직접 호출 - 위치 기반)
     */
    suspend fun getCharacterFromApi(lat: Double = 37.5665, lon: Double = 126.9780): Result<Character>

    /**
     * 캐릭터 정보 저장/업데이트
     */
    suspend fun saveCharacter(userId: Long, character: Character): Result<Unit>

    /**
     * userId로 캐릭터 정보 삭제
     */
    suspend fun deleteCharacter(userId: Long): Result<Unit>

    /**
     * 위치 기반 캐릭터 정보 조회
     *
     * @param lat 위도
     * @param lon 경도
     * @return 캐릭터 정보
     */
    suspend fun getCharacterByLocation(lat: Double, lon: Double): Result<Character>
}




