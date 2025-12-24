package team.swyp.sdu.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 알림 설정 보관용 DataStore
 *
 * FCM 알림 설정을 저장하고 관리합니다.
 */
@Singleton
class NotificationDataStore @Inject constructor(
    @Named("notification") private val dataStore: DataStore<Preferences>,
) {
    private val notificationEnabledKey = booleanPreferencesKey("notification_enabled")
    private val goalNotificationEnabledKey = booleanPreferencesKey("goal_notification_enabled")
    private val newMissionNotificationEnabledKey = booleanPreferencesKey("new_mission_notification_enabled")
    private val friendRequestNotificationEnabledKey = booleanPreferencesKey("friend_request_notification_enabled")
    private val marketingPushEnabledKey = booleanPreferencesKey("marketing_push_enabled")
    private val hasShownPermissionDialogKey = booleanPreferencesKey("has_shown_permission_dialog")

    val notificationEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[notificationEnabledKey] ?: true }
    val goalNotificationEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[goalNotificationEnabledKey] ?: true }
    val newMissionNotificationEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[newMissionNotificationEnabledKey] ?: true }
    val friendRequestNotificationEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[friendRequestNotificationEnabledKey] ?: true }
    val marketingPushEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[marketingPushEnabledKey] ?: true }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[notificationEnabledKey] = enabled }
    }

    suspend fun setGoalNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[goalNotificationEnabledKey] = enabled }
    }

    suspend fun setNewMissionNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[newMissionNotificationEnabledKey] = enabled }
    }

    suspend fun setFriendRequestNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[friendRequestNotificationEnabledKey] = enabled }
    }

    suspend fun setMarketingPushEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[marketingPushEnabledKey] = enabled }
    }

    /**
     * 로컬에 알림 설정이 저장되어 있는지 확인
     * 모든 필드가 기본값이 아닌 실제 값으로 저장되어 있으면 true 반환
     */
    suspend fun hasLocalSettings(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[notificationEnabledKey] != null ||
            prefs[goalNotificationEnabledKey] != null ||
            prefs[newMissionNotificationEnabledKey] != null ||
            prefs[friendRequestNotificationEnabledKey] != null ||
            prefs[marketingPushEnabledKey] != null
    }

    /**
     * 서버에서 받은 설정을 로컬에 저장
     */
    suspend fun saveSettings(
        notificationEnabled: Boolean,
        goalNotificationEnabled: Boolean,
        missionNotificationEnabled: Boolean,
        friendNotificationEnabled: Boolean,
        marketingPushEnabled: Boolean,
    ) {
        dataStore.edit { prefs ->
            prefs[notificationEnabledKey] = notificationEnabled
            prefs[goalNotificationEnabledKey] = goalNotificationEnabled
            prefs[newMissionNotificationEnabledKey] = missionNotificationEnabled
            prefs[friendRequestNotificationEnabledKey] = friendNotificationEnabled
            prefs[marketingPushEnabledKey] = marketingPushEnabled
        }
    }

    val hasShownPermissionDialog: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[hasShownPermissionDialogKey] ?: false }

    suspend fun setHasShownPermissionDialog(hasShown: Boolean) {
        dataStore.edit { prefs -> prefs[hasShownPermissionDialogKey] = hasShown }
    }
}

