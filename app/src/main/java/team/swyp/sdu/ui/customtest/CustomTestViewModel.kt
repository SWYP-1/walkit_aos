package team.swyp.sdu.ui.customtest

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.utils.WalkingTestData
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
