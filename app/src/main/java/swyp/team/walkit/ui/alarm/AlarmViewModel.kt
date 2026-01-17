package swyp.team.walkit.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.notification.NotificationRemoteDataSource
import swyp.team.walkit.data.remote.user.UserRemoteDataSource
import swyp.team.walkit.data.repository.NotificationRepository
import swyp.team.walkit.domain.model.AlarmType
import swyp.team.walkit.domain.repository.FriendRepository
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 알람 화면 ViewModel
 */
@HiltViewModel
class AlarmViewModel
@Inject
constructor(
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val notificationRepository: NotificationRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAlarms()
    }

    /**
     * 알람 목록 로드
     */
    fun loadAlarms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = notificationRemoteDataSource.getNotificationList(limit = 20)) {
                    is Result.Success -> {
                        val notificationList = result.data
                        _alarms.value = notificationList.map { dto ->
                            AlarmItem(
                                id = dto.notificationId.toString(),
                                type = parseAlarmType(dto.type),
                                message = dto.body,
                                date = formatDate(dto.createdAt),
                                senderNickname = dto.senderNickname,
                            )
                        }
                        Timber.d("알람 목록 로드 성공: ${_alarms.value.size}개")
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "알람 목록 로드 실패")
                        _alarms.value = emptyList()
                    }
                    Result.Loading -> {
                        // 로딩 중
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t, "알람 목록 로드 실패")
                _alarms.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 알람 타입 파싱
     */
    private fun parseAlarmType(type: String): AlarmType {
        return try {
            AlarmType.valueOf(type.uppercase())
        } catch (t: Throwable) {
            Timber.w(t, "알 수 없는 알람 타입: $type")
            AlarmType.FOLLOW // 기본값
        }
    }

    /**
     * 날짜 포맷팅
     */
    private fun formatDate(dateString: String?): String {
        if (dateString == null) return ""
        
        return try {
            // 다양한 ISO 8601 형식 시도
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                    date = inputFormat.parse(dateString)
                    if (date != null) break
                } catch (t: Throwable) {
                    // 다음 형식 시도
                }
            }
            
            // 한국어 형식으로 포맷팅
            val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (t: Throwable) {
            Timber.w(t, "날짜 파싱 실패: $dateString")
            dateString // 파싱 실패 시 원본 반환
        }
    }

    /**
     * 팔로우 확인 (수락)
     */
    fun confirmFollow(alarmId: String) {
        viewModelScope.launch {
            try {
                val alarm = _alarms.value.find { it.id == alarmId }
                if (alarm == null || alarm.senderNickname == null) {
                    Timber.w("알람을 찾을 수 없거나 senderNickname이 없습니다: $alarmId")
                    return@launch
                }

                Timber.d("팔로우 요청 수락 시작: ${alarm.senderNickname}")
                when (val result = friendRepository.acceptFollowRequest(alarm.senderNickname)) {
                    is Result.Success -> {
                        Timber.d("팔로우 요청 수락 성공: ${alarm.senderNickname}")

                        // 친구 목록 캐시 무효화 및 이벤트 발행
                        friendRepository.invalidateCache()
                        friendRepository.emitFriendUpdated()
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "팔로우 요청 수락 실패: ${alarm.senderNickname}")
                        throw result.exception ?: Exception("팔로우 요청 수락 실패")
                    }
                    Result.Loading -> {
                        // 로딩 상태
                    }
                }

                // 알람 목록에서 제거
                _alarms.value = _alarms.value.filter { it.id != alarmId }
            } catch (t: Throwable) {
                Timber.e(t, "팔로우 확인 실패: $alarmId")
                // 에러 발생 시에도 UI는 업데이트하지 않음 (사용자가 재시도할 수 있도록)
            }
        }
    }

    /**
     * 알람 삭제 (팔로우 요청 거절)
     */
    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            try {
                val alarm = _alarms.value.find { it.id == alarmId }
                if (alarm == null || alarm.senderNickname == null) {
                    Timber.w("알람을 찾을 수 없거나 senderNickname이 없습니다: $alarmId")
                    return@launch
                }

                Timber.d("팔로우 요청 거절 시작: ${alarm.senderNickname}")
                when (val result = friendRepository.rejectFollowRequest(alarm.senderNickname)) {
                    is Result.Success -> {
                        Timber.d("팔로우 요청 거절 성공: ${alarm.senderNickname}")

                        // 친구 목록 캐시 무효화 및 이벤트 발행 (거절의 경우도 목록 변경 가능성 대비)
                        friendRepository.invalidateCache()
                        friendRepository.emitFriendUpdated()
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "팔로우 요청 거절 실패: ${alarm.senderNickname}")
                        throw result.exception ?: Exception("팔로우 요청 거절 실패")
                    }
                    Result.Loading -> {
                        // 로딩 상태
                    }
                }

                // 알람 목록에서 제거
                _alarms.value = _alarms.value.filter { it.id != alarmId }
            } catch (t: Throwable) {
                Timber.e(t, "알람 삭제 실패: $alarmId")
                // 에러 발생 시에도 UI는 업데이트하지 않음 (사용자가 재시도할 수 있도록)
            }
        }
    }

    /**
     * 알림 삭제 (실제 알림 제거)
     */
    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("알림 삭제 시작: $notificationId")
                when (val result = notificationRepository.deleteNotification(notificationId)) {
                    is Result.Success -> {
                        Timber.d("알림 삭제 성공: $notificationId")
                        // 알람 목록에서 제거
                        _alarms.value = _alarms.value.filter { it.id != notificationId.toString() }
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "알림 삭제 실패: $notificationId")
                        // 에러 발생 시에도 UI는 업데이트하지 않음 (사용자가 재시도할 수 있도록)
                    }
                    Result.Loading -> {
                        // 로딩 상태 처리
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t, "알림 삭제 실패: $notificationId")
            }
        }
    }
}

