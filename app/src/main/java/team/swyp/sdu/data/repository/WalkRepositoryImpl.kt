package team.swyp.sdu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.remote.walking.WalkRemoteDataSource
import team.swyp.sdu.data.remote.walking.dto.FollowerWalkRecordDto
import team.swyp.sdu.data.remote.walking.dto.WalkSaveResponse
import team.swyp.sdu.domain.repository.WalkRepository
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
    ): Result<Response<WalkSaveResponse>> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.saveWalk(session, imageUri)
            } catch (e: Exception) {
                Timber.e(e, "산책 저장 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun getFollowerWalkRecord(
        nickname: String,
        lat: Double?,
        lon: Double?
    ): Result<FollowerWalkRecordDto> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.getFollowerWalkRecord(nickname, lat, lon)
            } catch (e: Exception) {
                Timber.e(e, "팔로워 산책 기록 조회 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun likeWalk(walkId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.likeWalk(walkId)
            } catch (e: Exception) {
                Timber.e(e, "산책 좋아요 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun unlikeWalk(walkId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.unlikeWalk(walkId)
            } catch (e: Exception) {
                Timber.e(e, "산책 좋아요 취소 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateWalkNote(walkId: Long, note: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                walkRemoteDataSource.updateWalkNote(walkId, note)
            } catch (e: Exception) {
                Timber.e(e, "산책 노트 업데이트 실패")
                Result.Error(e, e.message)
            }
        }
}

