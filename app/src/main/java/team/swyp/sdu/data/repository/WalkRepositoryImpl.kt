package team.swyp.sdu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.remote.walking.WalkRemoteDataSource
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.model.WalkSaveResult
import team.swyp.sdu.domain.repository.WalkRepository
import team.swyp.sdu.data.remote.walking.mapper.FollowerWalkRecordMapper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 산책 관련 Repository 구현체
 */
@Singleton
class WalkRepositoryImpl @Inject constructor(
    private val walkRemoteDataSource: WalkRemoteDataSource,
) : WalkRepository {

    override suspend fun saveWalk(
        session: WalkingSession,
        imageUri: String?
    ): Result<WalkSaveResult> =
        withContext(Dispatchers.IO) {
            try {
                val response = walkRemoteDataSource.saveWalk(session, imageUri)
                when (response) {
                    is Result.Success -> {
                        val domainResult = WalkSaveResult.toDomain(response.data)
                        Result.Success(domainResult)
                    }

                    is Result.Error -> response
                    Result.Loading -> Result.Loading
                }
            } catch (t: Throwable) {
                Timber.e(t, "산책 저장 실패")
                Result.Error(t, t.message)
            }
        }

    override suspend fun getFollowerWalkRecord(
        nickname: String,
        lat: Double?,
        lon: Double?
    ): Result<FollowerWalkRecord> =
        withContext(Dispatchers.IO) {
            try {
                val dtoResult = walkRemoteDataSource.getFollowerWalkRecord(nickname, lat, lon)
                when (dtoResult) {
                    is Result.Success -> {
                        val domainModel = FollowerWalkRecordMapper.toDomain(dtoResult.data)
                        Result.Success(domainModel)
                    }

                    is Result.Error -> {
                        // 서버 에러 코드에 따른 구체적인 처리
                        val exception = dtoResult.exception
                        val errorMessage = dtoResult.message ?: "알 수 없는 에러"

                        when (exception?.message) {
                            "NOT_FOLLOWING" -> {
                                // 404, 2001: 팔로워가 아닌 유저 조회
                                Timber.w("팔로워가 아닌 사용자 조회 시도: $nickname")
                                Result.Error(
                                    Exception("NOT_FOLLOWING"),
                                    "${nickname}님을 팔로우하고 있지 않습니다"
                                )
                            }
                            "NO_WALK_RECORDS" -> {
                                // 404, 5001: 산책 기록이 없는 경우
                                Timber.d("산책 기록이 없는 팔로워: $nickname")
                                Result.Error(
                                    Exception("NO_WALK_RECORDS"),
                                    "${nickname}님의 산책 기록이 아직 없습니다"
                                )
                            }
                            else -> {
                                // 기타 에러는 그대로 전달
                                Timber.e("팔로워 산책 기록 조회 에러: ${exception?.message}")
                                dtoResult
                            }
                        }
                    }

                    Result.Loading -> Result.Loading
                }
            } catch (t: Throwable) {
                Timber.e(t, "팔로워 산책 기록 조회 실패")
                Result.Error(t, t.message)
            }
        }

    override suspend fun likeWalk(walkId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.likeWalk(walkId)
            } catch (t: Throwable) {
                Timber.e(t, "산책 좋아요 실패")
                Result.Error(t, t.message)
            }
        }

    override suspend fun unlikeWalk(walkId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.unlikeWalk(walkId)
            } catch (t: Throwable) {
                Timber.e(t, "산책 좋아요 취소 실패")
                Result.Error(t, t.message)
            }
        }

    override suspend fun updateWalkNote(walkId: Long, note: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.updateWalkNote(walkId, note)
            } catch (t: Throwable) {
                Timber.e(t, "산책 노트 업데이트 실패")
                Result.Error(t, t.message)
            }
        }
}

