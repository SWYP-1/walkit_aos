package team.swyp.sdu.ui.mypage.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.repository.NotificationRepository
import team.swyp.sdu.data.remote.notification.dto.UpdateNotificationSettingsRequest
import timber.log.Timber

/**
 * 알림 설정 UI 상태
 */
sealed interface NotificationSettingsUiState {
    /**
     * 로딩 중 (DataStore에서 값을 읽는 중)
     */
    data object Loading : NotificationSettingsUiState

    /**
     * 준비 완료 (데이터 로드 완료)
     */
    data class Ready(
        val notificationEnabled: Boolean,
        val goalNotificationEnabled: Boolean,
        val missionNotificationEnabled: Boolean,
        val friendNotificationEnabled: Boolean,
        val marketingPushEnabled: Boolean,
        val isSaving: Boolean = false,
    ) : NotificationSettingsUiState
}

/**
 * 알림 설정 ViewModel
 *
 * - 서버에서 알림 설정 GET
 * - ViewModel에 로컬 복사본 생성
 * - 토글은 전부 로컬 상태만 변경
 * - 저장/화면 종료 시 diff 만들어서 PATCH
 */
@HiltViewModel
class NotificationSettingsViewModel
@Inject
constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    // 서버에서 가져온 원본 데이터 (변경 전 상태)
    private var originalSettings: NotificationSettingsUiState.Ready? = null
    private var isInitialLoad = true

    // 저장 중 상태 관리
    private val _isSaving = MutableStateFlow(false)

    // DataStore Flow를 직접 구독하여 초기값이 즉시 반영되도록 함
    // initialValue를 null로 설정하고 WhileSubscribed로 시작하여 깜빡임 방지
    private val _settingsState: StateFlow<NotificationSettingsUiState.Ready?> = 
        notificationRepository.getLocalNotificationSettingsFlow()
            .map { localState ->
                NotificationSettingsUiState.Ready(
                    notificationEnabled = localState.notificationEnabled,
                    goalNotificationEnabled = localState.goalNotificationEnabled,
                    missionNotificationEnabled = localState.missionNotificationEnabled,
                    friendNotificationEnabled = localState.friendNotificationEnabled,
                    marketingPushEnabled = localState.marketingPushEnabled,
                )
            }
            .onEach { uiState ->
                // 초기값 설정 (한 번만)
                if (isInitialLoad) {
                    originalSettings = uiState
                    isInitialLoad = false
                    // 로컬에 값이 없으면 서버에서 가져오기
                    checkAndLoadFromServer()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null, // 초기값 null로 설정하여 깜빡임 방지
            )

    // UI 상태: 설정 상태와 저장 중 상태를 결합
    // null인 경우 Loading, null이 아닌 경우 Ready
    val uiState: StateFlow<NotificationSettingsUiState> = 
        combine(
            _settingsState,
            _isSaving,
        ) { settings, saving ->
            if (settings == null) {
                NotificationSettingsUiState.Loading
            } else {
                settings.copy(isSaving = saving)
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NotificationSettingsUiState.Loading,
            )

    // ViewModel이 파괴되어도 저장 작업이 완료되도록 ApplicationScope 사용
    // SupervisorJob을 사용하여 자식 코루틴의 실패가 다른 코루틴에 영향을 주지 않도록 함
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 로컬에 값이 없으면 서버에서 가져오기
     * 초기값이 설정된 후 한 번만 호출됨
     */
    private fun checkAndLoadFromServer() {
        viewModelScope.launch {
            val hasLocalSettings = notificationRepository.hasLocalNotificationSettings()
            if (!hasLocalSettings) {
                Timber.d("로컬 설정이 없어 서버에서 가져옵니다")
                when (val result = notificationRepository.getNotificationSettings()) {
                    is Result.Success -> {
                        val settings = result.data
                        
                        // 서버 값을 DataStore에 저장 (Repository를 통해)
                        // DataStore에 저장하면 Flow가 자동으로 업데이트되어 UI에 반영됨
                        notificationRepository.saveLocalNotificationSettings(
                            notificationEnabled = settings.notificationEnabled,
                            goalNotificationEnabled = settings.goalNotificationEnabled,
                            missionNotificationEnabled = settings.missionNotificationEnabled,
                            friendNotificationEnabled = settings.friendNotificationEnabled,
                            marketingPushEnabled = settings.marketingPushEnabled,
                        )
                        
                        // 원본 설정 업데이트 (서버 값으로)
                        val serverState = NotificationSettingsUiState.Ready(
                            notificationEnabled = settings.notificationEnabled,
                            goalNotificationEnabled = settings.goalNotificationEnabled,
                            missionNotificationEnabled = settings.missionNotificationEnabled,
                            friendNotificationEnabled = settings.friendNotificationEnabled,
                            marketingPushEnabled = settings.marketingPushEnabled,
                        )
                        originalSettings = serverState
                        Timber.d("서버에서 알림 설정 로드 및 저장 성공")
                    }

                    is Result.Error -> {
                        Timber.e(result.exception, "알림 설정 로드 실패 (로컬 기본값 유지)")
                        // 에러 발생 시에도 로컬 기본값 유지
                    }

                    Result.Loading -> {
                        Timber.d("알림 설정 로드 중...")
                    }
                }
            } else {
                Timber.d("로컬 설정이 있어 서버 값으로 덮어쓰지 않습니다")
            }
        }
    }

    /**
     * 서버에서 알림 설정 GET (테스트용)
     */
    fun getNotificationSettings() {
        viewModelScope.launch {
            when (val result = notificationRepository.getNotificationSettings()) {
                is Result.Success -> {
                    val settings = result.data
                    Timber.d("알림 설정 GET 성공: $settings")
                    
                    // 서버 값을 DataStore에 저장
                    notificationRepository.saveLocalNotificationSettings(
                        notificationEnabled = settings.notificationEnabled,
                        goalNotificationEnabled = settings.goalNotificationEnabled,
                        missionNotificationEnabled = settings.missionNotificationEnabled,
                        friendNotificationEnabled = settings.friendNotificationEnabled,
                        marketingPushEnabled = settings.marketingPushEnabled,
                    )
                    
                    // 원본 설정 업데이트
                    val serverState = NotificationSettingsUiState.Ready(
                        notificationEnabled = settings.notificationEnabled,
                        goalNotificationEnabled = settings.goalNotificationEnabled,
                        missionNotificationEnabled = settings.missionNotificationEnabled,
                        friendNotificationEnabled = settings.friendNotificationEnabled,
                        marketingPushEnabled = settings.marketingPushEnabled,
                    )
                    originalSettings = serverState
                }
                is Result.Error -> {
                    Timber.e(result.exception, "알림 설정 GET 실패")
                }
                Result.Loading -> {
                    Timber.d("알림 설정 GET 중...")
                }
            }
        }
    }

    /**
     * 전체 알림 토글 변경 (DataStore 업데이트)
     * DataStore가 업데이트되면 Flow가 자동으로 UI에 반영됨
     */
    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.updateLocalNotificationEnabled(enabled)
        }
    }

    /**
     * 목표 알림 토글 변경 (DataStore 업데이트)
     */
    fun setGoalNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.updateLocalGoalNotificationEnabled(enabled)
        }
    }

    /**
     * 미션 알림 토글 변경 (DataStore 업데이트)
     */
    fun setMissionNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.updateLocalMissionNotificationEnabled(enabled)
        }
    }

    /**
     * 친구 알림 토글 변경 (DataStore 업데이트)
     */
    fun setFriendNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.updateLocalFriendNotificationEnabled(enabled)
        }
    }

    /**
     * 마케팅 푸시 동의 토글 변경 (DataStore 업데이트)
     */
    fun setMarketingPushEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.updateLocalMarketingPushEnabled(enabled)
        }
    }

    /**
     * 변경사항 저장 (전체 상태를 PATCH)
     * 서버에는 현재 상태의 모든 값을 전송
     */
    fun saveSettings() {
        val currentState = uiState.value
        val current = (currentState as? NotificationSettingsUiState.Ready) ?: return

        // 변경사항이 없으면 저장하지 않음
        val original = originalSettings
        if (original != null) {
            val hasChanges = current.notificationEnabled != original.notificationEnabled ||
                    current.goalNotificationEnabled != original.goalNotificationEnabled ||
                    current.missionNotificationEnabled != original.missionNotificationEnabled ||
                    current.friendNotificationEnabled != original.friendNotificationEnabled ||
                    current.marketingPushEnabled != original.marketingPushEnabled

            if (!hasChanges) {
                Timber.d("변경사항이 없어 저장하지 않음")
                return
            }
        }

        // 전체 상태를 서버에 전송 (모든 필드 포함)
        val request = UpdateNotificationSettingsRequest(
            notificationEnabled = current.notificationEnabled,
            goalNotificationEnabled = current.goalNotificationEnabled,
            newMissionNotificationEnabled = current.missionNotificationEnabled,
            friendNotificationEnabled = current.friendNotificationEnabled,
            marketingPushEnabled = current.marketingPushEnabled,
        )

        // 저장 중 상태 설정
        _isSaving.value = true

        // 실제 저장 작업은 ApplicationScope에서 실행하여 ViewModel 파괴 후에도 완료되도록 보장
        applicationScope.launch {
            try {
                when (val result = notificationRepository.updateNotificationSettings(request)) {
                    is Result.Success -> {
                        // 저장 성공 시 원본 데이터 업데이트
                        viewModelScope.launch {
                            originalSettings = current.copy(isSaving = false)
                            _isSaving.value = false
                        }
                        Timber.d("알림 설정 저장 성공 (전체 상태를 서버에 전송함)")
                    }

                    is Result.Error -> {
                        // 에러 발생 시 저장 중 상태 해제
                        viewModelScope.launch {
                            _isSaving.value = false
                        }
                        Timber.e(result.exception, "알림 설정 저장 실패")
                    }

                    Result.Loading -> {
                        // Loading 상태 유지
                        Timber.d("알림 설정 저장 중...")
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 저장 중 상태 해제
                viewModelScope.launch {
                    _isSaving.value = false
                }
                Timber.e(e, "알림 설정 저장 중 예외 발생")
            }
        }
    }

}

