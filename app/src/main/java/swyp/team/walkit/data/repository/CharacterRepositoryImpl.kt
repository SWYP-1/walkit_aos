package swyp.team.walkit.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.local.dao.CharacterDao
import swyp.team.walkit.data.local.mapper.CharacterMapper
import swyp.team.walkit.data.remote.auth.CharacterRemoteDataSource
import swyp.team.walkit.data.remote.walking.mapper.CharacterMapper as RemoteCharacterMapper
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import retrofit2.HttpException
import java.io.IOException
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

    override fun observeCharacter(userId: Long): Flow<Character?> =
        characterDao.observeCharacter(userId)
            .map { entity -> entity?.let(CharacterMapper::toDomain) }

    override suspend fun getCharacterFromDb(userId: Long): Character? =
        withContext(Dispatchers.IO) {
            try {
                val entity = characterDao.getCharacter(userId)
                entity?.let(CharacterMapper::toDomain)
            } catch (e: Exception) {
                // DB 조회 실패는 치명적이지 않으므로 null 반환
                Timber.e(e, "DB에서 캐릭터 정보 조회 실패: $userId")
                null
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun getCharacter(userId: Long): Result<Character> =
        withContext(Dispatchers.IO) {
            try {
                val entity = characterDao.getCharacter(userId)
                if (entity != null) {
                    val character = CharacterMapper.toDomain(entity)
                    Result.Success(character)
                } else {
                    // DB에 없으면 API 호출
                    Timber.d("DB에 캐릭터 정보 없음, API 호출 시도:")
                    val apiResult = getCharacterFromApi()
                    if (apiResult is Result.Success) {
                        // API에서 가져온 캐릭터 정보를 DB에 저장 (userId으로)
                        saveCharacter(userId, apiResult.data)
                        Result.Success(apiResult.data)
                    } else {
                        Result.Error(
                            Exception("캐릭터 정보를 찾을 수 없습니다"),
                            "캐릭터 정보를 찾을 수 없습니다: $userId"
                        )
                    }
                }
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "getCharacter")
                Timber.e(e, "캐릭터 정보 조회 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "getCharacter")
                Timber.e(e, "캐릭터 정보 조회 실패: HTTP ${e.code()}")
                Result.Error(e, "캐릭터 정보를 불러올 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun getCharacterFromApi(lat: Double, lon: Double): Result<Character> =
        withContext(Dispatchers.IO) {
            try {
                val dto = characterRemoteDataSource.getCharacterByLocation(lat, lon)
                val character = RemoteCharacterMapper.toDomain(dto)
                Timber.d("API에서 캐릭터 정보 조회 성공: lat=$lat, lon=$lon")
                Result.Success(character)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "getCharacterFromApi")
                Timber.e(e, "API에서 캐릭터 정보 조회 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "getCharacterFromApi")
                Timber.e(e, "API에서 캐릭터 정보 조회 실패: HTTP ${e.code()}")
                Result.Error(e, "캐릭터 정보를 불러올 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun saveCharacter(
        userId: Long,
        character: Character,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = CharacterMapper.toEntity(character, userId)
                characterDao.upsert(entity)
                Timber.d("캐릭터 정보 저장 성공: userId=$userId, grade=${character.grade}")
                Result.Success(Unit)
            } catch (e: Exception) {
                // DB 저장 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "캐릭터 정보 저장 실패: userId=$userId")
                Result.Error(e, "캐릭터 정보 저장에 실패했습니다")
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun deleteCharacter(userId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                characterDao.deleteByUserId(userId)
                Timber.d("캐릭터 정보 삭제 성공: userId=$userId")
                Result.Success(Unit)
            } catch (e: Exception) {
                // DB 삭제 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "캐릭터 정보 삭제 실패: userId=$userId")
                Result.Error(e, "캐릭터 정보 삭제에 실패했습니다")
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun getCharacterByLocation(lat: Double, lon: Double): Result<Character> {
        return try {
            val dto = characterRemoteDataSource.getCharacterByLocation(lat, lon)
            val character = RemoteCharacterMapper.toDomain(dto)
            Timber.d("위치 기반 캐릭터 정보 조회 성공: lat=$lat, lon=$lon, nickname=${character.nickName}")
            Result.Success(character)
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "getCharacterByLocation")
            Timber.e(e, "위치 기반 캐릭터 정보 조회 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "getCharacterByLocation")
            Timber.e(e, "위치 기반 캐릭터 정보 조회 실패: HTTP ${e.code()}")
            Result.Error(e, "캐릭터 정보를 불러올 수 없습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }
}




