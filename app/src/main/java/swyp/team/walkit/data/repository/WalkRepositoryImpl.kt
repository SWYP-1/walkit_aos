package swyp.team.walkit.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.remote.walking.WalkRemoteDataSource
import swyp.team.walkit.domain.model.FollowerWalkRecord
import swyp.team.walkit.domain.model.WalkSaveResult
import swyp.team.walkit.domain.repository.WalkRepository
import swyp.team.walkit.data.remote.walking.mapper.FollowerWalkRecordMapper
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import java.io.IOException
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "saveWalk")
                Timber.e(e, "산책 저장 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "saveWalk")
                Timber.e(e, "산책 저장 실패: HTTP ${e.code()}")
                Result.Error(e, "산책 저장에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "getFollowerWalkRecord")
                Timber.e(e, "팔로워 산책 기록 조회 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "getFollowerWalkRecord")
                Timber.e(e, "팔로워 산책 기록 조회 실패: HTTP ${e.code()}")
                Result.Error(e, "산책 기록을 불러올 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun likeWalk(walkId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.likeWalk(walkId)
                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "likeWalk")
                Timber.e(e, "산책 좋아요 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "likeWalk")
                Timber.e(e, "산책 좋아요 실패: HTTP ${e.code()}")
                Result.Error(e, "좋아요에 실패했습니다")
            }
        }

    override suspend fun unlikeWalk(walkId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.unlikeWalk(walkId)
                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "unlikeWalk")
                Timber.e(e, "산책 좋아요 취소 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "unlikeWalk")
                Timber.e(e, "산책 좋아요 취소 실패: HTTP ${e.code()}")
                Result.Error(e, "좋아요 취소에 실패했습니다")
            }
        }

    override suspend fun getWalkList(): Result<List<WalkingSession>> =
        withContext(Dispatchers.IO) {
            try {
                val dtoResult = walkRemoteDataSource.getWalkList()
                when (dtoResult) {
                    is Result.Success -> {
                        val walkingSessions = dtoResult.data
                        Timber.d("산책 목록 조회 성공: ${walkingSessions.size}개")
                        Result.Success(walkingSessions)
                    }

                    is Result.Error -> dtoResult
                    Result.Loading -> Result.Loading
                }
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "getWalkList")
                Timber.e(e, "산책 목록 조회 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "getWalkList")
                Timber.e(e, "산책 목록 조회 실패: HTTP ${e.code()}")
                Result.Error(e, "산책 목록을 불러올 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun updateWalkNote(walkId: Long, note: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.updateWalkNote(walkId, note)
                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "updateWalkNote")
                Timber.e(e, "산책 노트 업데이트 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "updateWalkNote")
                Timber.e(e, "산책 노트 업데이트 실패: HTTP ${e.code()}")
                Result.Error(e, "노트 업데이트에 실패했습니다")
            }
        }
}

