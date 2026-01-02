package team.swyp.sdu.ui.home

import android.content.Context
import android.content.pm.PackageManager
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
import team.swyp.sdu.data.local.datastore.LocationAgreementDataStore
import team.swyp.sdu.domain.service.LocationManager
import timber.log.Timber
import javax.inject.Inject

/**
 * 위치 동의 다이얼로그 UI 상태
 */
sealed interface LocationAgreementUiState {
    data object Idle : LocationAgreementUiState
    data object Checking : LocationAgreementUiState
    data object ShouldShowDialog : LocationAgreementUiState // 다이얼로그 표시 필요
    data object Requesting : LocationAgreementUiState
    data object Granted : LocationAgreementUiState
    data object Denied : LocationAgreementUiState
}

/**
 * 위치 동의 요청 ViewModel
 */
@HiltViewModel
class LocationAgreementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager,
    private val locationAgreementDataStore: LocationAgreementDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationAgreementUiState>(LocationAgreementUiState.Idle)
    val uiState: StateFlow<LocationAgreementUiState> = _uiState.asStateFlow()

    /**
     * 다이얼로그 표시 여부 확인
     * 산책 버튼 클릭 시 호출
     */
    fun checkShouldShowDialog() {
        viewModelScope.launch {
            _uiState.value = LocationAgreementUiState.Checking

            // 이미 위치 권한이 있으면 다이얼로그 표시 안 함
            if (locationManager.hasLocationPermission()) {
                Timber.d("위치 권한이 이미 있으므로 다이얼로그 표시 안 함")
                _uiState.value = LocationAgreementUiState.Idle
                return@launch
            }

            // 이미 다이얼로그를 표시했는지 확인
            val hasShownDialog = locationAgreementDataStore.hasShownDialog()
            if (hasShownDialog) {
                Timber.d("이미 다이얼로그를 표시했으므로 다시 표시 안 함")
                _uiState.value = LocationAgreementUiState.Idle
                return@launch
            }

            // 다이얼로그 표시 필요
            Timber.d("위치 동의 다이얼로그 표시")
            _uiState.value = LocationAgreementUiState.ShouldShowDialog
        }
    }

    /**
     * 다이얼로그 표시 (직접 호출용)
     */
    fun showDialog() {
        _uiState.value = LocationAgreementUiState.ShouldShowDialog
    }

    /**
     * 다이얼로그 닫기
     */
    fun dismissDialog() {
        viewModelScope.launch {
            // 다이얼로그를 표시했다고 기록
            locationAgreementDataStore.setHasShownLocationAgreementDialog(true)
            _uiState.value = LocationAgreementUiState.Idle
        }
    }

    /**
     * 권한 동의 (다이얼로그에서 "동의하기" 버튼 클릭 시)
     */
    fun grantPermission() {
        _uiState.value = LocationAgreementUiState.Requesting
        // 실제 권한 요청은 Activity에서 처리
    }

    /**
     * 권한 거부 (다이얼로그에서 "나중에" 버튼 클릭 시)
     * "나중에 할게요"를 선택한 경우 다시 시도할 수 있도록 다이얼로그 표시 플래그를 저장하지 않음
     */
    fun denyPermission() {
        viewModelScope.launch {
            // "나중에 할게요" 선택 시 다이얼로그 표시 플래그를 저장하지 않음 (다시 시도 가능)
            // locationAgreementDataStore.setHasShownLocationAgreementDialog(true) // 제거됨
            _uiState.value = LocationAgreementUiState.Denied
            Timber.d("위치 권한 요청을 나중에로 미룸 - 다음에 다시 시도 가능")
        }
    }

    /**
     * 권한 요청 결과 처리
     */
    fun handlePermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                _uiState.value = LocationAgreementUiState.Granted
                Timber.d("위치 권한 승인됨")
            } else {
                _uiState.value = LocationAgreementUiState.Denied
                Timber.d("위치 권한 거부됨")
            }

            // 다이얼로그를 표시했다고 기록
            locationAgreementDataStore.setHasShownLocationAgreementDialog(true)
        }
    }

    /**
     * 위치 권한이 있는지 확인
     */
    fun hasLocationPermission(): Boolean {
        return locationManager.hasLocationPermission()
    }
}


