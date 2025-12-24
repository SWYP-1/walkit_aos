package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.data.local.datastore.NotificationDataStore

/**
 * 알림 설정 UI 상태
 */
data class NotificationSettingsUiState(
    val notificationEnabled: Boolean = true,
    val goalNotificationEnabled: Boolean = true,
    val newMissionNotificationEnabled: Boolean = true,
    val friendRequestNotificationEnabled: Boolean = true,
)

/**
 * 알림 설정 ViewModel
 */
@HiltViewModel
class NotificationSettingsViewModel
    @Inject
    constructor(
        private val notificationDataStore: NotificationDataStore,
    ) : ViewModel() {
        val uiState: StateFlow<NotificationSettingsUiState> =
            combine(
                notificationDataStore.notificationEnabled,
                notificationDataStore.goalNotificationEnabled,
                notificationDataStore.newMissionNotificationEnabled,
                notificationDataStore.friendRequestNotificationEnabled,
            ) { notification, goal, mission, friendRequest ->
                NotificationSettingsUiState(
                    notificationEnabled = notification,
                    goalNotificationEnabled = goal,
                    newMissionNotificationEnabled = mission,
                    friendRequestNotificationEnabled = friendRequest,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = NotificationSettingsUiState(),
            )

        fun setNotificationEnabled(enabled: Boolean) {
            viewModelScope.launch {
                notificationDataStore.setNotificationEnabled(enabled)
            }
        }

        fun setGoalNotificationEnabled(enabled: Boolean) {
            viewModelScope.launch {
                notificationDataStore.setGoalNotificationEnabled(enabled)
            }
        }

        fun setNewMissionNotificationEnabled(enabled: Boolean) {
            viewModelScope.launch {
                notificationDataStore.setNewMissionNotificationEnabled(enabled)
            }
        }

        fun setFriendRequestNotificationEnabled(enabled: Boolean) {
            viewModelScope.launch {
                notificationDataStore.setFriendRequestNotificationEnabled(enabled)
            }
        }
    }



