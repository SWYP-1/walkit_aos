package team.swyp.sdu.data.repository

import team.swyp.sdu.data.local.dao.WalkingSessionDao
import team.swyp.sdu.data.local.mapper.WalkingSessionMapper
import team.swyp.sdu.data.model.WalkingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WalkingSession Repository
 *
 * 로컬 저장소와 서버 동기화를 추상화하는 Repository입니다.
 * 현재는 로컬 저장만 구현하고, 서버 동기화는 스텁으로 구현되어 있습니다.
 */
@Singleton
class WalkingSessionRepository
    @Inject
    constructor(
        private val walkingSessionDao: WalkingSessionDao,
    ) {
        /**
         * 세션 저장 (로컬 저장 + 서버 동기화 시도)
         */
        suspend fun saveSession(session: WalkingSession): Long =
            try {
                // 로컬에 먼저 저장
                val entity = WalkingSessionMapper.toEntity(session, isSynced = false)
                val id = walkingSessionDao.insert(entity)
                Timber.d("세션 로컬 저장 완료: ID=$id")

                // 서버 동기화 시도 (비동기, 실패해도 로컬 저장은 유지)
                try {
                    syncToServer(session)
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
         * 서버 동기화 (스텁)
         *
         * TODO: 실제 서버 API 연동 시 이 함수를 구현하세요.
         * - Retrofit을 사용한 API 호출
         * - 성공 시 isSynced = true로 업데이트
         * - 실패 시 재시도 로직 구현
         */
        suspend fun syncToServer(session: WalkingSession) {
            // TODO: 서버 API 연동 구현 필요
            // 예시:
            // try {
            //     val response = apiService.uploadSession(session)
            //     if (response.isSuccessful) {
            //         updateSyncStatus(session.id, true)
            //     }
            // } catch (e: Exception) {
            //     Timber.e(e, "서버 동기화 실패")
            //     throw e
            // }

            Timber.d("서버 동기화 스텁 호출됨 (실제 구현 필요): 세션 시작 시간=${session.startTime}")
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
    }
