package swyp.team.walkit.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐릭터 관련 이벤트 버스
 * ViewModel 간 통신을 위한 싱글톤 이벤트 버스
 */
@Singleton
class CharacterEventBus @Inject constructor() {

    private val _characterUpdated = MutableSharedFlow<Unit>(replay = 0)
    val characterUpdated: SharedFlow<Unit> = _characterUpdated.asSharedFlow()

    /**
     * 캐릭터 착용 상태가 변경되었음을 알림
     */
    suspend fun notifyCharacterUpdated() {
        _characterUpdated.emit(Unit)
    }
}
