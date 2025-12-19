package team.swyp.sdu.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import team.swyp.sdu.data.api.walking.WalkApi
import team.swyp.sdu.data.local.dao.WalkingSessionDao
import team.swyp.sdu.data.local.mapper.WalkingSessionMapper
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.remote.walking.dto.WalkSaveRequest
import team.swyp.sdu.data.remote.walking.dto.WalkSaveResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WalkingSession Repository
 *
 * 로컬 저장소와 서버 동기화를 추상화하는 Repository입니다.
 */
@Singleton
class WalkingSessionRepository
    @Inject
    constructor(
        private val walkingSessionDao: WalkingSessionDao,
        private val walkApi: WalkApi,
        @ApplicationContext private val context: Context,
        private val gson: Gson,
    ) {
        /**
         * 세션 저장 (로컬 저장 + 서버 동기화 시도)
         *
         * @param session 저장할 산책 세션
         * @param imageUri 산책 이미지 URI (선택사항)
         * @return 저장된 세션의 로컬 ID
         */
        suspend fun saveSession(
            session: WalkingSession,
            imageUri: Uri? = null,
        ): Long =
            try {
                // 로컬에 먼저 저장
                val entity = WalkingSessionMapper.toEntity(session, isSynced = false)
                val id = walkingSessionDao.insert(entity)
                Timber.d("세션 로컬 저장 완료: ID=$id")

                // 서버 동기화 시도 (비동기, 실패해도 로컬 저장은 유지)
                try {
                    syncToServer(session, imageUri, id)
                } catch (e: Exception) {
                    Timber.w(e, "서버 동기화 실패 (로컬 저장은 유지됨)")
                }

                id
            } catch (e: Exception) {
                Timber.e(e, "세션 저장 실패")
                throw e
            }

        /**
         * 모든 세션 조회 (Flow로 실시간 업데이트)
         */
        fun getAllSessions(): Flow<List<WalkingSession>> =
            walkingSessionDao
                .getAllSessions()
                .map { entities ->
                    entities.map { WalkingSessionMapper.toDomain(it) }
                }

        /**
         * 기간 내 세션 조회
         */
        fun getSessionsBetween(
            startMillis: Long,
            endMillis: Long,
        ): Flow<List<WalkingSession>> =
            walkingSessionDao
                .getSessionsBetween(startMillis, endMillis)
                .map { entities -> entities.map { WalkingSessionMapper.toDomain(it) } }

        /**
         * ID로 세션 조회
         */
        suspend fun getSessionById(id: Long): WalkingSession? =
            walkingSessionDao.getSessionById(id)?.let { WalkingSessionMapper.toDomain(it) }

        /**
         * 세션 삭제
         */
        suspend fun deleteSession(id: Long) {
            try {
                walkingSessionDao.deleteById(id)
                Timber.d("세션 삭제 완료: ID=$id")
            } catch (e: Exception) {
                Timber.e(e, "세션 삭제 실패: ID=$id")
                throw e
            }
        }

        /**
         * 서버 동기화
         *
         * @param session 동기화할 산책 세션
         * @param imageUri 산책 이미지 URI (선택사항)
         * @param localId 로컬 데이터베이스 ID (동기화 상태 업데이트용)
         */
        private suspend fun syncToServer(
            session: WalkingSession,
            imageUri: Uri?,
            localId: Long,
        ) = withContext(Dispatchers.IO) {
            try {
                // 필수 필드 검증
                require(session.endTime != null) { "endTime이 null입니다" }
                require(session.preWalkEmotion != null) { "preWalkEmotion이 null입니다" }
                require(session.postWalkEmotion != null) { "postWalkEmotion이 null입니다" }

                // LocationPoint를 WalkPoint로 변환
                val walkPoints = WalkSaveRequest.fromLocationPoints(session.locations)

                // WalkSaveRequest 생성
                val request = WalkSaveRequest(
                    preWalkEmotion = session.preWalkEmotion.name,
                    postWalkEmotion = session.postWalkEmotion.name,
                    note = session.note,
                    startTime = session.startTime,
                    endTime = session.endTime!!,
                    stepCount = session.stepCount,
                    totalDistance = session.totalDistance,
                    points = walkPoints,
                )

                // JSON으로 직렬화
                val requestJson = gson.toJson(request)
                val requestBody = requestJson.toRequestBody("application/json".toMediaType())

                // 이미지 파일 처리
                val imagePart: MultipartBody.Part? = imageUri?.let { uri ->
                    try {
                        val file = uriToFile(uri)
                        val requestFile = file.asRequestBody("image/*".toMediaType())
                        MultipartBody.Part.createFormData("image", file.name, requestFile)
                    } catch (e: Exception) {
                        Timber.w(e, "이미지 파일 변환 실패")
                        null
                    }
                }

                // API 호출
                val response: WalkSaveResponse = walkApi.saveWalk(requestBody, imagePart)

                if (response.success && response.data != null) {
                    // 서버 응답에서 받은 정보로 세션 업데이트
                    val updatedSession = session.copy(
                        imageUrl = response.data.imageUrl,
                        createdDate = response.data.createdDate,
                    )

                    // 로컬 DB 업데이트
                    val baseEntity = WalkingSessionMapper.toEntity(updatedSession, isSynced = true)
                    val updatedEntity = baseEntity.copy(id = localId)
                    walkingSessionDao.update(updatedEntity)

                    // 동기화 상태 업데이트
                    updateSyncStatus(localId, true)

                    Timber.d("서버 동기화 성공: 세션 ID=${response.data.id}, 이미지 URL=${response.data.imageUrl}")
                } else {
                    Timber.w("서버 동기화 실패: ${response.message}")
                    throw Exception(response.message ?: "서버 동기화 실패")
                }
            } catch (e: Exception) {
                Timber.e(e, "서버 동기화 중 오류 발생")
                throw e
            }
        }

        /**
         * URI를 File로 변환
         */
        private suspend fun uriToFile(uri: Uri): File = withContext(Dispatchers.IO) {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "walk_image_${System.currentTimeMillis()}.jpg")

            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("URI에서 InputStream을 가져올 수 없습니다")

            file
        }

        /**
         * 미동기화 세션 모두 동기화 (스텁)
         *
         * TODO: 실제 서버 API 연동 시 이 함수를 구현하세요.
         */
        suspend fun syncAllPendingSessions() {
            // TODO: 서버 API 연동 구현 필요
            // val unsyncedSessions = walkingSessionDao.getUnsyncedSessions()
            // unsyncedSessions.forEach { entity ->
            //     try {
            //         syncToServer(entity.toDomain())
            //     } catch (e: Exception) {
            //         Timber.w(e, "세션 동기화 실패: ID=${entity.id}")
            //     }
            // }

            Timber.d("미동기화 세션 동기화 스텁 호출됨 (실제 구현 필요)")
        }

        /**
         * 동기화 상태 업데이트
         */
        private suspend fun updateSyncStatus(
            id: Long,
            isSynced: Boolean,
        ) {
            walkingSessionDao.updateSyncStatus(id, isSynced)
        }

        /**
         * 총 걸음수 조회 (Flow로 실시간 업데이트)
         */
        fun getTotalStepCount(): Flow<Int> = walkingSessionDao.getTotalStepCount()

        /**
         * 총 이동거리 조회 (Flow로 실시간 업데이트, 미터 단위)
         */
        fun getTotalDistance(): Flow<Float> = walkingSessionDao.getTotalDistance()
    }
