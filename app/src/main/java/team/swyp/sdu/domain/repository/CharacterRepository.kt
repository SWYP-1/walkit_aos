package team.swyp.sdu.domain.repository

import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Character

/**
 * 캐릭터 정보 Repository 인터페이스
 *
 * Home API에서 받은 Character 정보를 Room에 저장하고 조회합니다.
 * nickname을 primary key로 사용하여 User와 1:1 관계를 가집니다.
 */
interface CharacterRepository {
    /**
     * nickname으로 캐릭터 정보 조회 (Flow)
     */
    fun observeCharacter(nickname: String): Flow<Character?>

    /**
     * nickname으로 캐릭터 정보 조회
     */
    suspend fun getCharacter(nickname: String): Result<Character>

    /**
     * 캐릭터 정보 저장/업데이트
     */
    suspend fun saveCharacter(nickname: String, character: Character): Result<Unit>

    /**
     * nickname으로 캐릭터 정보 삭제
     */
    suspend fun deleteCharacter(nickname: String): Result<Unit>
}

