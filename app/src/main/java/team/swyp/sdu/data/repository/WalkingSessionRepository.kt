package team.swyp.sdu.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import team.swyp.sdu.data.local.dao.WalkingSessionDao
import team.swyp.sdu.data.local.mapper.WalkingSessionMapper
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.remote.walking.WalkRemoteDataSource
import team.swyp.sdu.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.getOrNull
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.data.local.dao.UserDao
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
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
    private val walkRemoteDataSource: WalkRemoteDataSource,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context,
) {

    /**
     * 현재 사용자 ID 가져오기
     */
    suspend fun getCurrentUserId(): Long {
        val userEntity = userDao.getCurrentUser()
        val userId = userEntity?.userId ?: 0L
        Timber.d("getCurrentUserId: userEntity=$userEntity, userId=$userId")
        return userId
    }

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
    ): String {
        val userId = getCurrentUserId()

        // userId 검증 - 로그인하지 않은 사용자는 세션 저장 불가
        if (userId == 0L) {
            Timber.e("사용자 정보가 없어 세션 저장 불가: userId=$userId")
            throw IllegalStateException("로그인이 필요합니다. 세션을 저장할 수 없습니다.")
        }

        Timber.d("세션 저장 시작: userId=$userId, sessionId=${session.id}")

        // 1. 로컬 저장 (PENDING) - userId 설정
        val entity = WalkingSessionMapper.toEntity(
            session.copy(userId = userId),
            syncState = SyncState.PENDING
        )
        val sessionUUID = session.id

        // 2. 서버 동기화 시도
        try {
            walkingSessionDao.updateSyncState(
                sessionUUID,
                SyncState.SYNCING
            )

            syncToServer(session, userId,imageUri)

            walkingSessionDao.updateSyncState(
                sessionUUID,
                SyncState.SYNCED
            )
        } catch (e: Exception) {
            walkingSessionDao.updateSyncState(
                sessionUUID,
                SyncState.FAILED
            )
            Timber.w(e, "서버 동기화 실패")
        }

        return sessionUUID
    }

    /**
     * 세션 로컬 전용 저장 (데이터베이스에만 저장, 서버 동기화 없음)
     *
     * 더미 데이터나 테스트용 데이터 저장에 사용
     *
     * @param session 저장할 산책 세션
     * @param imageUri 산책 이미지 URI (선택사항)
     * @param syncState 동기화 상태 (기본값: NONE - 동기화하지 않음)
     * @return 저장된 세션의 로컬 ID
     */
    suspend fun saveSessionLocalOnly(
        session: WalkingSession,
        imageUri: Uri? = null,
        syncState: SyncState = SyncState.SYNCED,
    ): String {
        // 1. 로컬 저장 (지정된 syncState로)
        val entity = WalkingSessionMapper.toEntity(
            session,
            syncState = syncState
        )
        walkingSessionDao.insert(entity)

        // 2. 서버 동기화는 하지 않음
        Timber.d("로컬 전용 세션 저장 완료: Id=${session.id}, syncState=$syncState")
        return session.id
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
    ): Flow<List<WalkingSession>> {
        // 현재 사용자 ID로 필터링
        return userDao.observeCurrentUser()
            .map { entity -> entity?.userId ?: 0L }
            .flatMapLatest { userId ->
                walkingSessionDao
                    .getSessionsBetweenForUser(userId, startMillis, endMillis)
                    .map { entities -> entities.map { WalkingSessionMapper.toDomain(it) } }
            }
    }

    /**
     * ID로 세션 관찰 (Flow로 실시간 업데이트) - 현재 사용자만
     */
    fun observeSessionById(id: String): Flow<WalkingSession?> {
        return userDao.observeCurrentUser()
            .map { entity -> entity?.userId ?: 0L }
            .flatMapLatest { userId ->
                walkingSessionDao.observeSessionByIdForUser(userId, id)
                    .map { entity -> entity?.let { WalkingSessionMapper.toDomain(it) } }
            }
    }

    /**
     * ID로 세션 조회 - 현재 사용자만
     */
    suspend fun getSessionById(id: String): WalkingSession? {
        val userId = getCurrentUserId()
        return walkingSessionDao.getSessionByIdForUser(userId, id)
            ?.let { WalkingSessionMapper.toDomain(it) }
    }

    /**
     * 세션 삭제
     */
    suspend fun deleteSession(id: String) {
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
        userId : Long,
        imageUri: Uri?,
    ) = withContext(Dispatchers.IO) {
        val imageUriString = imageUri?.toString()
        val result = walkRemoteDataSource.saveWalk(session, imageUriString)

        when (result) {
            is Result.Success -> {
                val response = result.data
                val serverImageUrl = response.imageUrl

                // Entity 업데이트 (serverImageUrl 저장)
                val entity = walkingSessionDao.getSessionByIdForUser(userId,session.id)
                if (entity != null && serverImageUrl != null) {
                    val updatedEntity = entity.copy(
                        serverImageUrl = serverImageUrl
                        // localImagePath는 유지 (오프라인 지원)
                    )
                    walkingSessionDao.update(updatedEntity)
                }
                // 서버 동기화 성공
                // 로컬 DB의 동기화 상태는 이미 SYNCED로 업데이트됨
                Timber.d("서버 동기화 성공: 세션 ID=${session.id}, serverImageUrl=$serverImageUrl")
            }

            is Result.Error -> {
                Timber.e(result.exception, "서버 동기화 중 오류 발생: ${result.message}")
                throw result.exception
            }

            is Result.Loading -> {
                // 이 경우는 발생하지 않아야 하지만, 안전을 위해 처리
                Timber.w("서버 동기화가 로딩 상태입니다")
            }
        }
    }

    /**
     * 미동기화 세션 조회 (PENDING 또는 FAILED 상태)
     */
    suspend fun getUnsyncedSessions(): List<WalkingSession> {
        val userId = getCurrentUserId()
        val entities = walkingSessionDao.getUnsyncedSessionsForUser(userId)
        return entities.map { WalkingSessionMapper.toDomain(it) }
    }

    /**
     * 동기화된 세션 조회 (SYNCED 상태)
     */
    suspend fun getSyncedSessions(): List<WalkingSession> {
        val userId = getCurrentUserId()
        val entities = walkingSessionDao.getSyncedSessionsForUser(userId)
        return entities.map { WalkingSessionMapper.toDomain(it) }
    }

    /**
     * 미동기화 세션 모두 동기화 (WorkManager에서 호출)
     */
    suspend fun syncAllPendingSessions() {
        val unsyncedSessions = getUnsyncedSessions()

        if (unsyncedSessions.isEmpty()) {
            Timber.d("동기화할 세션이 없습니다")
            return
        }
        val userId = getCurrentUserId()

        Timber.d("미동기화 세션 ${unsyncedSessions.size}개 발견, 동기화 시작")

        unsyncedSessions.forEach { session ->
            try {
                // 동기화 상태를 SYNCING으로 변경
                walkingSessionDao.updateSyncState(session.id, SyncState.SYNCING)

                // 서버 동기화 시도 (이미지 URI는 null로 전달)
                syncToServer(session, userId,null)

                // 성공 시 SYNCED로 변경
                walkingSessionDao.updateSyncState(session.id, SyncState.SYNCED)
                Timber.d("세션 동기화 성공: ID=${session.id}")

            } catch (e: Exception) {
                // 실패 시 FAILED로 변경
                walkingSessionDao.updateSyncState(session.id, SyncState.FAILED)
                Timber.w(e, "세션 동기화 실패: ID=${session.id}")
            }
        }

        Timber.d("미동기화 세션 동기화 완료")
    }


    /**
     * 총 걸음수 조회 (Flow로 실시간 업데이트)
     */
    fun getTotalStepCount(): Flow<Int> = walkingSessionDao.getTotalStepCount()

    /**
     * 총 이동거리 조회 (Flow로 실시간 업데이트, 미터 단위)
     */
    fun getTotalDistance(): Flow<Float> = walkingSessionDao.getTotalDistance()

    /**
     * 총 산책 시간 조회 (Flow로 실시간 업데이트, 밀리초 단위)
     */
    fun getTotalDuration(): Flow<Long> = walkingSessionDao.getTotalDuration()

    /**
     * 부분 세션 생성 (stopWalking() 실행 시 즉시 호출)
     *
     * @param session 기본 세션 데이터
     *   - postWalkEmotion: 선택되지 않았으면 preWalkEmotion과 동일
     *   - localImagePath: null (나중에 업데이트)
     *   - serverImageUrl: null (서버 동기화 후 업데이트)
     *   - note: null (나중에 업데이트)
     * @return 저장된 세션의 로컬 ID
     */
    suspend fun createSessionPartial(session: WalkingSession): String {
        // ✅ userId 검증 - 로그인하지 않은 사용자는 세션 저장 불가
        if (session.userId == 0L) {
            Timber.e("세션 userId가 0입니다. 로그인이 필요합니다. 세션을 저장할 수 없습니다.")
            throw IllegalStateException("로그인이 필요합니다. 세션을 저장할 수 없습니다.")
        }

        // 1. 로컬 저장 (PENDING 상태)
        val entity = WalkingSessionMapper.toEntity(
            session = session,
            syncState = SyncState.PENDING
        )
        walkingSessionDao.insert(entity)

        // 서버 동기화는 하지 않음 (아직 완료되지 않았으므로)

        Timber.d("부분 세션 저장 완료: Id=${session.id}, userId=${session.userId}")
        return session.id
    }

    /**
     * 세션의 산책 후 감정 업데이트 (PostWalkingEmotionScreen에서 선택 시)
     *
     * @param localId 업데이트할 세션의 로컬 ID
     * @param postWalkEmotion 선택된 산책 후 감정
     */
    suspend fun updatePostWalkEmotion(
        localId: String,
        postWalkEmotion: EmotionType
    ) {
        val userId = getCurrentUserId()
        Timber.d("산책 후 감정 업데이트 시도: localId=$localId, userId=$userId, postEmotion=$postWalkEmotion")

        // 먼저 현재 사용자의 세션으로 찾기
        var entity = walkingSessionDao.getSessionByIdForUser(userId, localId)

        // 찾을 수 없으면 userId=0인 임시 세션으로 찾기 (마이그레이션용)
        if (entity == null) {
            Timber.w("현재 사용자 세션 찾을 수 없음, 임시 세션(userId=0)에서 검색: localId=$localId")
            entity = walkingSessionDao.getSessionByIdForUser(0L, localId)

            // 임시 세션을 찾았으면 현재 사용자 ID로 업데이트
            if (entity != null) {
                Timber.d("임시 세션 발견, userId 업데이트: localId=$localId, oldUserId=${entity.userId} -> newUserId=$userId")
                val updatedEntity = entity.copy(userId = userId)
                walkingSessionDao.update(updatedEntity)
                entity = updatedEntity
            }
        }

        if (entity == null) {
            throw IllegalStateException("세션을 찾을 수 없습니다: ID=$localId, userId=$userId")
        }

        val updatedEntity = entity.copy(
            postWalkEmotion = postWalkEmotion.name
        )

        walkingSessionDao.update(updatedEntity)

        Timber.d("산책 후 감정 업데이트 완료: localId=$localId, emotion=$postWalkEmotion")
    }

    /**
     * URI를 파일로 복사하고 경로 반환
     *
     * **두 가지 경우 처리:**
     * 1. **카메라 촬영**: MediaStore에 저장된 이미지 (content://media/...)
     * 2. **갤러리 선택**: 갤러리 앱의 이미지 (content://media/... 또는 content://com.android.providers.media.documents/...)
     *
     * **왜 복사가 필요한가?**
     * - 카메라 촬영: MediaStore에 저장되어 있지만, 사용자가 갤러리에서 삭제할 수 있음
     * - 갤러리 선택: 다른 앱의 파일을 참조하므로 권한 문제나 파일 삭제 가능성 있음
     * - **앱 내부 저장소에 복사하면**: 앱과 함께 관리되며, 삭제 시점을 제어할 수 있음
     *
     * @param uri 복사할 이미지 URI (content:// 또는 file://)
     * @return 저장된 파일의 절대 경로 (실패 시 null)
     */
    private suspend fun copyImageUriToFile(uri: Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // URI 스킴 확인
            val scheme = uri.scheme
            Timber.d("이미지 URI 스킴: $scheme, URI: $uri")

            // Content URI인 경우 (카메라 촬영 또는 갤러리 선택)
            val inputStream = when {
                scheme == "content" -> {
                    context.contentResolver.openInputStream(uri)
                }

                scheme == "file" -> {
                    // File URI인 경우 (드물지만 가능)
                    File(uri.path ?: return@withContext null).inputStream()
                }

                else -> {
                    Timber.w("지원하지 않는 URI 스킴: $scheme")
                    return@withContext null
                }
            } ?: return@withContext null

            // 파일명 생성 (타임스탬프 기반)
            val timestamp = System.currentTimeMillis()
            val fileName = "walking_image_${timestamp}.jpg"

            // 앱 내부 저장소의 Pictures 디렉토리에 저장
            // getExternalFilesDir: 앱 전용 외부 저장소 (앱 삭제 시 함께 삭제됨)
            // filesDir: 앱 내부 저장소 (항상 사용 가능)
            val fileDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val file = File(fileDir, fileName)

            // 파일 복사
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }

            val absolutePath = file.absolutePath
            Timber.d("이미지 파일 복사 완료: $absolutePath (원본 URI: $uri)")
            absolutePath
        } catch (e: Exception) {
            Timber.e(e, "이미지 파일 복사 실패: $uri")
            null
        }
    }

    /**
     * 세션의 이미지와 노트 업데이트 (사진/텍스트 단계)
     *
     * @param localId 업데이트할 세션의 로컬 ID
     * @param imageUri 이미지 URI (선택사항, null이면 기존 값 유지)
     * @param note 노트 텍스트 (선택사항, null이면 기존 값 유지)
     */
    suspend fun updateSessionImageAndNote(
        localId: String,
        imageUri: Uri? = null,
        note: String? = null,
    ) {
        val userId = getCurrentUserId()
        val entity = walkingSessionDao.getSessionByIdForUser(userId, localId)
        if (entity == null) {
            Timber.w("세션을 찾을 수 없습니다 - 이미 삭제되었거나 존재하지 않음: ID=$localId")
            return // 조용히 리턴하여 앱 크래시 방지
        }

        // URI를 파일 경로로 변환하여 localImagePath에 저장
        val localImagePath = imageUri?.let { copyImageUriToFile(it) }

        val updatedEntity = entity.copy(
            localImagePath = localImagePath ?: entity.localImagePath, // 로컬 파일 경로 저장
            note = note ?: entity.note
        )

        walkingSessionDao.update(updatedEntity)

        Timber.d("세션 이미지/노트 업데이트 완료: localId=$localId, localImagePath=$localImagePath, note=$note")

        // 서버 동기화는 WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 처리
    }

    /**
     * 세션을 서버와 동기화 (WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 호출)
     *
     * @param localId 동기화할 세션의 로컬 ID
     */
    suspend fun syncSessionToServer(localId: String) {
        val userId = getCurrentUserId()
        val entity = walkingSessionDao.getSessionByIdForUser(userId,localId)
            ?: throw IllegalStateException("세션을 찾을 수 없습니다: ID=$localId")

        // 이미 동기화된 경우 스킵
        if (entity.syncState == SyncState.SYNCED) {
            Timber.d("이미 동기화된 세션: localId=$localId")
            return
        }

        // Domain 모델로 변환
        val session = WalkingSessionMapper.toDomain(entity)

        // 로컬 이미지 파일 경로를 URI로 변환 (서버 업로드용)
        val imageUri = entity.localImagePath?.let { imagePath ->
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    // FileProvider를 사용하여 URI 생성 (Android 7.0+ 호환)
                    // FileProvider는 AndroidManifest.xml에 선언되어 있어야 함
                    val authority = "${context.packageName}.fileprovider"
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        authority,
                        file
                    )
                } else {
                    Timber.w("이미지 파일이 존재하지 않음: $imagePath")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "이미지 URI 변환 실패: $imagePath")
                null
            }
        }

        // 이미지 URI 로깅 (디버깅용)
        if (imageUri != null) {
            Timber.d("서버 동기화: 이미지 URI 생성 완료 - $imageUri")
        } else {
            Timber.d("서버 동기화: 이미지 URI가 null입니다 (이미지 없이 동기화)")
        }

        // 동기화 상태를 SYNCING으로 변경
        walkingSessionDao.updateSyncState(localId, SyncState.SYNCING)

        try {
            // 서버 동기화 시도 (imageUri를 String으로 변환하여 전달)
            val imageUriString = imageUri?.toString()
            Timber.d("서버 동기화: imageUriString=$imageUriString")
            val result = walkRemoteDataSource.saveWalk(session, imageUriString)

            when (result) {
                is Result.Success -> {
                    val response = result.data
                    val serverImageUrl = response.imageUrl

                    val updatedEntity = entity.copy(
                        serverImageUrl = serverImageUrl // 서버에서 받은 URL 저장
                        // localImagePath는 유지 (오프라인 지원)
                    )
                    walkingSessionDao.update(updatedEntity)

                    // 동기화 성공
                    walkingSessionDao.updateSyncState(localId, SyncState.SYNCED)
                    Timber.d("서버 동기화 성공: localId=$localId, serverImageUrl=$serverImageUrl")
                }

                is Result.Error -> {
                    throw result.exception
                }

                is Result.Loading -> {
                    Timber.w("서버 동기화가 로딩 상태입니다")
                }
            }
        } catch (e: Exception) {
            // CancellationException인 경우 (ViewModel이 destroy되거나 화면을 벗어난 경우)
            // 취소된 경우에는 PENDING 상태로 되돌려서 나중에 재시도 가능하도록 함
            if (e is CancellationException) {
                walkingSessionDao.updateSyncState(localId, SyncState.PENDING)
                Timber.w("서버 동기화 취소됨 (재시도 가능): localId=$localId")
                // 취소 예외는 다시 throw하지 않음 (정상적인 취소이므로)
                return
            }

            // 실제 서버 에러인 경우에만 FAILED 상태로 변경
            walkingSessionDao.updateSyncState(localId, SyncState.FAILED)
            Timber.e(e, "서버 동기화 실패: localId=$localId")
            throw e
        }
    }

    suspend fun updateSessionNote(id: String, newNote: String) {
        val userId = getCurrentUserId()
        val entity = walkingSessionDao.getSessionByIdForUser(userId,id)
        if (entity == null) {
            Timber.w("세션을 찾을 수 없습니다 - 이미 삭제되었거나 존재하지 않음: ID=$id")
            return // 조용히 리턴하여 앱 크래시 방지
        }

        val updatedEntity = entity.copy(note = newNote)
        walkingSessionDao.update(updatedEntity)

        Timber.d("세션 노트 업데이트 완료: localId: $id : note : $newNote")

    }
}
