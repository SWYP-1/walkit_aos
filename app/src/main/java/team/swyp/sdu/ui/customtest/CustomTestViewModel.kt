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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.d("🚀 더미 세션 데이터 추가 시작")
                val dummySessions = WalkingTestData.generateTestSessions() // 40개 생성
                Timber.d("📊 생성된 더미 세션 수: ${dummySessions.size}")

                dummySessions.forEachIndexed { index, session ->
                    Timber.d("💾 [${index + 1}/${dummySessions.size}] 세션 저장 시도: ${session.startTime}")
                    walkingSessionRepository.saveSessionLocalOnly(
                        session = session,
                        imageUri = null, // 이미지 없이 저장
                        syncState = SyncState.SYNCED // 서버 동기화하지 않음
                    )
                    Timber.d("✅ [${index + 1}/${dummySessions.size}] 세션 저장 완료: ${session.id}")
                }

                Timber.d("🎉 더미 세션 데이터 추가 완료: ${dummySessions.size}개 (로컬 전용)")
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "❌ 더미 세션 데이터 추가 실패: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
