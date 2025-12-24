package team.swyp.sdu.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import team.swyp.sdu.data.local.datastore.NotificationDataStore
import team.swyp.sdu.data.repository.NotificationRepository
import team.swyp.sdu.data.remote.notification.dto.UpdateNotificationSettingsRequest
import timber.log.Timber
import javax.inject.Inject

/**
 * 알림 권한 요청 UI 상태
 */
sealed interface NotificationPermissionUiState {
    data object Idle : NotificationPermissionUiState
    data object Checking : NotificationPermissionUiState
    data object ShouldShowDialog : NotificationPermissionUiState // 다이얼로그 표시 필요
    data object Requesting : NotificationPermissionUiState
    data object Granted : NotificationPermissionUiState
    data object Denied : NotificationPermissionUiState
}

/**
 * 알림 권한 요청 ViewModel
 */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val notificationDataStore: NotificationDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow<NotificationPermissionUiState>(NotificationPermissionUiState.Idle)
    val uiState: StateFlow<NotificationPermissionUiState> = _uiState.asStateFlow()

    /**
     * 다이얼로그 표시 여부 확인
     * 홈 화면 진입 시 호출
     */
    fun checkShouldShowDialog() {
        viewModelScope.launch {
            _uiState.value = NotificationPermissionUiState.Checking

            // Android 13 미만이면 다이얼로그 표시 안 함
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                _uiState.value = NotificationPermissionUiState.Idle
                return@launch
            }

            // 이미 권한이 있으면 다이얼로그 표시 안 함
            if (hasNotificationPermission()) {
                _uiState.value = NotificationPermissionUiState.Idle
                return@launch
            }

            // 이미 다이얼로그를 표시했는지 확인
            val hasShownDialog = notificationDataStore.hasShownPermissionDialog.first()
            if (hasShownDialog) {
                _uiState.value = NotificationPermissionUiState.Idle
                return@launch
            }

            // 다이얼로그 표시 필요
            _uiState.value = NotificationPermissionUiState.ShouldShowDialog
        }
    }

    /**
     * 다이얼로그 표시
     */
    fun showDialog() {
        _uiState.value = NotificationPermissionUiState.ShouldShowDialog
    }

    /**
     * 다이얼로그 닫기
     */
    fun dismissDialog() {
        viewModelScope.launch {
            // 다이얼로그를 표시했다고 기록
            notificationDataStore.setHasShownPermissionDialog(true)
            _uiState.value = NotificationPermissionUiState.Idle
        }
    }

    /**
     * 권한 요청 (다이얼로그에서 "알림 켜기" 버튼 클릭 시)
     */
    fun requestPermission() {
        _uiState.value = NotificationPermissionUiState.Requesting
        // 실제 권한 요청은 Activity에서 처리
    }

    /**
     * 권한 결과 처리
     */
    fun handlePermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                _uiState.value = NotificationPermissionUiState.Granted
                // 서버에 notification-consent = true 전송
                sendNotificationConsentToServer(true)
            } else {
                _uiState.value = NotificationPermissionUiState.Denied
                // 서버에 notification-consent = false 전송
                sendNotificationConsentToServer(false)
            }

            // 다이얼로그를 표시했다고 기록
            notificationDataStore.setHasShownPermissionDialog(true)
        }
    }

    /**
     * 나중에 버튼 클릭
     */
    fun skipPermission() {
        viewModelScope.launch {
            // 다이얼로그를 표시했다고 기록
            notificationDataStore.setHasShownPermissionDialog(true)
            _uiState.value = NotificationPermissionUiState.Idle
        }
    }

    /**
     * 알림 권한이 있는지 확인
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 이하는 항상 true
        }
    }

    /**
     * 서버에 알림 동의 여부 전송
     */
    private suspend fun sendNotificationConsentToServer(consent: Boolean) {
        try {
            val request = UpdateNotificationSettingsRequest(
                notificationEnabled = consent,
                goalNotificationEnabled = consent,
                newMissionNotificationEnabled = consent,
                friendNotificationEnabled = consent,
                marketingPushEnabled = consent,
            )
            val result = notificationRepository.updateNotificationSettings(request)
            when (result) {
                is team.swyp.sdu.core.Result.Success -> {
                    Timber.d("알림 동의 여부 서버 전송 성공: $consent")
                }
                is team.swyp.sdu.core.Result.Error -> {
                    Timber.e(result.exception, "알림 동의 여부 서버 전송 실패: ${result.message}")
                }
                team.swyp.sdu.core.Result.Loading -> {
                    // 처리 불필요
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "알림 동의 여부 서버 전송 실패")
        }
    }
}

