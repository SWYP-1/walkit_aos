package team.swyp.sdu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.local.dao.CharacterDao
import team.swyp.sdu.data.local.mapper.CharacterMapper
import team.swyp.sdu.data.remote.auth.CharacterRemoteDataSource
import team.swyp.sdu.data.remote.walking.mapper.CharacterMapper as RemoteCharacterMapper
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.repository.CharacterRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐릭터 정보 Repository 구현체
 *
 * Home API에서 받은 Character 정보를 Room에 저장하고 조회합니다.
 */
@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val characterDao: CharacterDao,
    private val characterRemoteDataSource: CharacterRemoteDataSource,
) : CharacterRepository {

    override fun observeCharacter(nickname: String): Flow<Character?> =
        characterDao.observeCharacter(nickname)
            .map { entity -> entity?.let(CharacterMapper::toDomain) }

    override suspend fun getCharacterFromDb(nickname: String): Character? =
        withContext(Dispatchers.IO) {
            try {
                val entity = characterDao.getCharacter(nickname)
                entity?.let(CharacterMapper::toDomain)
            } catch (e: Exception) {
                Timber.e(e, "DB에서 캐릭터 정보 조회 실패: $nickname")
                null
            }
        }

    override suspend fun getCharacter(nickname: String): Result<Character> =
        withContext(Dispatchers.IO) {
            try {
                val entity = characterDao.getCharacter(nickname)
                if (entity != null) {
                    val character = CharacterMapper.toDomain(entity)
                    Result.Success(character)
                } else {
                    // DB에 없으면 API 호출
                    Timber.d("DB에 캐릭터 정보 없음, API 호출 시도: $nickname")
                    val apiResult = getCharacterFromApi()
                    if (apiResult is Result.Success) {
                        // API에서 가져온 캐릭터 정보를 DB에 저장 (nickname으로)
                        saveCharacter(nickname, apiResult.data)
                        Result.Success(apiResult.data)
                    } else {
                        Result.Error(
                            Exception("캐릭터 정보를 찾을 수 없습니다"),
                            "캐릭터 정보를 찾을 수 없습니다: $nickname"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "캐릭터 정보 조회 실패: $nickname")
                Result.Error(e, e.message)
            }
        }

    override suspend fun getCharacterFromApi(lat: Double, lon: Double): Result<Character> =
        withContext(Dispatchers.IO) {
            try {
                val dto = characterRemoteDataSource.getCharacterByLocation(lat, lon)
                val character = RemoteCharacterMapper.toDomain(dto)
                Timber.d("API에서 캐릭터 정보 조회 성공: lat=$lat, lon=$lon")
                Result.Success(character)
            } catch (e: Exception) {
                Timber.e(e, "API에서 캐릭터 정보 조회 실패: lat=$lat, lon=$lon")
                Result.Error(e, e.message ?: "캐릭터 정보 조회 실패")
            }
        }

    override suspend fun saveCharacter(
        nickname: String,
        character: Character,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = CharacterMapper.toEntity(character, nickname)
                characterDao.upsert(entity)
                Timber.d("캐릭터 정보 저장 성공: nickname=$nickname, grade=${character.grade}")
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "캐릭터 정보 저장 실패: nickname=$nickname")
                Result.Error(e, e.message)
            }
        }

    override suspend fun deleteCharacter(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                characterDao.deleteByNickname(nickname)
                Timber.d("캐릭터 정보 삭제 성공: nickname=$nickname")
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "캐릭터 정보 삭제 실패: nickname=$nickname")
                Result.Error(e, e.message)
            }
        }

    override suspend fun getCharacterByLocation(lat: Double, lon: Double): Result<Character> {
        return try {
            val dto = characterRemoteDataSource.getCharacterByLocation(lat, lon)
            val character = RemoteCharacterMapper.toDomain(dto)
            Timber.d("위치 기반 캐릭터 정보 조회 성공: lat=$lat, lon=$lon, nickname=${character.nickName}")
            Result.Success(character)
        } catch (e: Exception) {
            Timber.e(e, "위치 기반 캐릭터 정보 조회 실패: lat=$lat, lon=$lon")
            Result.Error(e, e.message)
        }
    }
}




