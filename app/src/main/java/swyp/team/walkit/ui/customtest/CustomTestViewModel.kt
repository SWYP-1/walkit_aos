package swyp.team.walkit.ui.customtest

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.data.local.entity.SyncState
import swyp.team.walkit.utils.WalkingTestData
import timber.log.Timber
import javax.inject.Inject

/**
 * 커스텀 테스트 ViewModel
 */
@HiltViewModel
class CustomTestViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
) : ViewModel() {

    /**
     * 더미 세션 데이터 추가 (데이터베이스에만 저장, 서버 동기화 없음)
     */
    fun addDummySessions(onComplete: () -> Unit = {}) {

    }
}
