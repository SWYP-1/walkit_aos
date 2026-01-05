package team.swyp.sdu.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.local.dao.NotificationSettingsDao
import team.swyp.sdu.data.local.datastore.NotificationDataStore
import team.swyp.sdu.data.local.entity.NotificationSettingsEntity
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.data.remote.notification.NotificationRemoteDataSource
import team.swyp.sdu.data.remote.notification.dto.NotificationSettingsDto
import team.swyp.sdu.data.remote.notification.dto.UpdateNotificationSettingsRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 관련 Repository
 *
 * 로컬 저장소와 서버 동기화를 추상화하는 Repository입니다.
 */
/**
 * 알림 설정 UI 상태 (Repository에서 사용)
 */
data class NotificationSettingsState(
    val notificationEnabled: Boolean,
    val goalNotificationEnabled: Boolean,
    val missionNotificationEnabled: Boolean,
    val friendNotificationEnabled: Boolean,
    val marketingPushEnabled: Boolean,
)

@Singleton
class NotificationRepository @Inject constructor(
    private val remoteDataSource: NotificationRemoteDataSource,
    private val notificationSettingsDao: NotificationSettingsDao,
    private val notificationDataStore: NotificationDataStore,
) {
    /**
     * FCM 토큰 등록
     */
    suspend fun registerFcmToken(
        token: String,
        deviceId: String,
    ): Result<Unit> = remoteDataSource.registerFcmToken(token, deviceId)

    /**
     * 로컬 DataStore에서 알림 설정 Flow 조회
     * Flow를 직접 구독하여 초기값이 즉시 반영되도록 함
     */
    fun getLocalNotificationSettingsFlow(): Flow<NotificationSettingsState> {
        return combine(
            notificationDataStore.notificationEnabled,
            notificationDataStore.goalNotificationEnabled,
            notificationDataStore.newMissionNotificationEnabled,
            notificationDataStore.friendRequestNotificationEnabled,
            notificationDataStore.marketingPushEnabled,
        ) { notification, goal, mission, friend, marketing ->
            NotificationSettingsState(
                notificationEnabled = notification,
                goalNotificationEnabled = goal,
                missionNotificationEnabled = mission,
                friendNotificationEnabled = friend,
                marketingPushEnabled = marketing,
            )
        }
    }

    /**
     * 로컬 DataStore에서 알림 설정 조회 (suspend 함수)
     */
    suspend fun getLocalNotificationSettings(): NotificationSettingsState {
        return NotificationSettingsState(
            notificationEnabled = notificationDataStore.notificationEnabled.first(),
            goalNotificationEnabled = notificationDataStore.goalNotificationEnabled.first(),
            missionNotificationEnabled = notificationDataStore.newMissionNotificationEnabled.first(),
            friendNotificationEnabled = notificationDataStore.friendRequestNotificationEnabled.first(),
            marketingPushEnabled = notificationDataStore.marketingPushEnabled.first(),
        )
    }

    /**
     * 로컬에 알림 설정이 저장되어 있는지 확인
     */
    suspend fun hasLocalNotificationSettings(): Boolean {
        return notificationDataStore.hasLocalSettings()
    }

    /**
     * 로컬 DataStore에 알림 설정 저장
     */
    suspend fun saveLocalNotificationSettings(
        notificationEnabled: Boolean,
        goalNotificationEnabled: Boolean,
        missionNotificationEnabled: Boolean,
        friendNotificationEnabled: Boolean,
        marketingPushEnabled: Boolean,
    ) {
        notificationDataStore.saveSettings(
            notificationEnabled = notificationEnabled,
            goalNotificationEnabled = goalNotificationEnabled,
            missionNotificationEnabled = missionNotificationEnabled,
            friendNotificationEnabled = friendNotificationEnabled,
            marketingPushEnabled = marketingPushEnabled,
        )
    }

    /**
     * 개별 알림 설정 업데이트 (로컬 DataStore)
     */
    suspend fun updateLocalNotificationEnabled(enabled: Boolean) {
        notificationDataStore.setNotificationEnabled(enabled)
    }

    suspend fun updateLocalGoalNotificationEnabled(enabled: Boolean) {
        notificationDataStore.setGoalNotificationEnabled(enabled)
    }

    suspend fun updateLocalMissionNotificationEnabled(enabled: Boolean) {
        notificationDataStore.setNewMissionNotificationEnabled(enabled)
    }

    suspend fun updateLocalFriendNotificationEnabled(enabled: Boolean) {
        notificationDataStore.setFriendRequestNotificationEnabled(enabled)
    }

    suspend fun updateLocalMarketingPushEnabled(enabled: Boolean) {
        notificationDataStore.setMarketingPushEnabled(enabled)
    }

    /**
     * 알림 설정 조회 (서버에서)
     */
    suspend fun getNotificationSettings(): Result<NotificationSettingsDto> =
        remoteDataSource.getNotificationSettings()

    /**
     * 알림 설정 업데이트 (서버에만)
     *
     * ViewModel에서 사용하는 메서드로, 서버에만 업데이트합니다.
     * 로컬 동기화가 필요한 경우 saveNotificationSettings를 사용하세요.
     *
     * @param settings 업데이트할 알림 설정
     */
    suspend fun updateNotificationSettings(
        settings: UpdateNotificationSettingsRequest,
    ): Result<Unit> = remoteDataSource.updateNotificationSettings(settings)

    /**
     * 알림 설정 저장 및 동기화
     *
     * 1. 로컬에 PENDING 상태로 저장
     * 2. 서버 동기화 시도
     * 3. 성공 시 SYNCED, 실패 시 FAILED 상태로 업데이트
     *
     * @param settings 저장할 알림 설정
     */
    suspend fun saveNotificationSettings(
        settings: UpdateNotificationSettingsRequest,
    ) {
        // 1. 로컬 저장 (PENDING 상태)
        val entity = NotificationSettingsEntity(
            id = 1,
            notificationEnabled = settings.notificationEnabled ?: false,
            goalNotificationEnabled = settings.goalNotificationEnabled ?: false,
            missionNotificationEnabled = settings.newMissionNotificationEnabled ?: false,
            friendNotificationEnabled = settings.friendNotificationEnabled ?: false,
            marketingPushEnabled = settings.marketingPushEnabled ?: false,
            syncState = SyncState.PENDING,
            updatedAt = System.currentTimeMillis(),
        )
        notificationSettingsDao.insertOrUpdate(entity)

        // 2. 서버 동기화 시도
        try {
            notificationSettingsDao.updateSyncState(
                syncState = SyncState.SYNCING,
                updatedAt = System.currentTimeMillis(),
            )

            syncToServer(settings)

            notificationSettingsDao.updateSyncState(
                syncState = SyncState.SYNCED,
                updatedAt = System.currentTimeMillis(),
            )
            Timber.d("알림 설정 서버 동기화 성공")
        } catch (e: Exception) {
            // CancellationException인 경우 PENDING 상태로 되돌림 (재시도 가능)
            if (e is CancellationException) {
                notificationSettingsDao.updateSyncState(
                    syncState = SyncState.PENDING,
                    updatedAt = System.currentTimeMillis(),
                )
                Timber.w("알림 설정 동기화 취소됨 (재시도 가능)")
                return
            }

            // 실제 서버 에러인 경우에만 FAILED 상태로 변경
            notificationSettingsDao.updateSyncState(
                syncState = SyncState.FAILED,
                updatedAt = System.currentTimeMillis(),
            )
            Timber.e(e, "알림 설정 서버 동기화 실패")
            throw e
        }
    }

    /**
     * 서버 동기화 (내부 메서드)
     */
    private suspend fun syncToServer(
        settings: UpdateNotificationSettingsRequest,
    ) = withContext(Dispatchers.IO) {
        when (val result = remoteDataSource.updateNotificationSettings(settings)) {
            is Result.Success -> {
                Timber.d("알림 설정 서버 동기화 성공")
            }
            is Result.Error -> {
                Timber.e(result.exception, "알림 설정 서버 동기화 실패")
                throw result.exception
            }

            Result.Loading -> {
                Timber.d("알림 설정 서버 동기화 중")
            }
        }
    }

    /**
     * 동기화되지 않은 알림 설정 조회 (WorkManager용)
     */
    suspend fun getUnsyncedSettings(): NotificationSettingsEntity? =
        notificationSettingsDao.getUnsyncedSettings()

    /**
     * 동기화되지 않은 알림 설정 재시도
     */
    suspend fun retrySync() {
        val unsynced = getUnsyncedSettings() ?: return

        val settings = UpdateNotificationSettingsRequest(
            notificationEnabled = unsynced.notificationEnabled,
            goalNotificationEnabled = unsynced.goalNotificationEnabled,
            newMissionNotificationEnabled = unsynced.missionNotificationEnabled,
            friendNotificationEnabled = unsynced.friendNotificationEnabled,
            marketingPushEnabled = unsynced.marketingPushEnabled,
        )

        try {
            notificationSettingsDao.updateSyncState(
                syncState = SyncState.SYNCING,
                updatedAt = System.currentTimeMillis(),
            )

            syncToServer(settings)

            notificationSettingsDao.updateSyncState(
                syncState = SyncState.SYNCED,
                updatedAt = System.currentTimeMillis(),
            )
            Timber.d("알림 설정 재동기화 성공")
        } catch (e: Exception) {
            if (e is CancellationException) {
                notificationSettingsDao.updateSyncState(
                    syncState = SyncState.PENDING,
                    updatedAt = System.currentTimeMillis(),
                )
                return
            }

            notificationSettingsDao.updateSyncState(
                syncState = SyncState.FAILED,
                updatedAt = System.currentTimeMillis(),
            )
            Timber.e(e, "알림 설정 재동기화 실패")
            throw e
        }
    }

    /**
     * 알림 삭제
     *
     * @param notificationId 삭제할 알림 ID
     * @return Response<Unit>
     */
    suspend fun deleteNotification(notificationId: Long): Result<Unit> =
        remoteDataSource.deleteNotification(notificationId)
}

