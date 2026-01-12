package swyp.team.walkit.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 이벤트 버스
 * 401 Unauthorized 응답 시 로그인 화면으로 이동하기 위한 이벤트 버스
 */
@Singleton
class AuthEventBus @Inject constructor() {

    private val _requireLogin = MutableSharedFlow<Unit>(replay = 0)
    val requireLogin: SharedFlow<Unit> = _requireLogin.asSharedFlow()

    /**
     * 로그인이 필요함을 알림 (401 응답 등)
     */
    suspend fun notifyRequireLogin() {
        _requireLogin.emit(Unit)
    }
}


