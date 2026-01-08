package swyp.team.walkit.ui.customtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.data.local.entity.SyncState
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.ui.walking.utils.emotionTypeToString
import swyp.team.walkit.utils.DateUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * 커스텀 테스트 ViewModel
 */
@HiltViewModel
class CustomTestViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    /**
     * 더미 세션 데이터 추가 (데이터베이스에만 저장, 서버 동기화 없음)
     * 40개의 더미 데이터를 다양한 날짜에 추가
     */
    fun addDummySessions(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                Timber.d("🧪 CustomTestViewModel: 더미 세션 데이터 추가 시작 (40개)")

                // 현재 사용자 정보 가져오기
                val currentUser = userRepository.getUser()
                val userId = when (currentUser) {
                    is Result.Success -> {
                        Timber.d("✅ 현재 사용자 ID: ${currentUser.data.userId}")
                        currentUser.data.userId
                    }
                    is Result.Error -> {
                        Timber.w("⚠️ 사용자 정보를 가져올 수 없음, 기본값 0L 사용: ${currentUser.message}")
                        0L
                    }

                    Result.Loading -> TODO()
                }

                // 40일 전부터 시작해서 40개의 데이터 생성
                val startDate = System.currentTimeMillis() - 40 * 24 * 60 * 60 * 1000L
                val today = DateUtils.getStartOfDay(System.currentTimeMillis())

                var createdCount = 0

                // 40일간의 데이터 생성 (최근 40일)
                for (dayIndex in 0 until 40) {
                    val targetDate = startDate + (dayIndex * 24 * 60 * 60 * 1000L)

                    // 오늘 날짜는 제외
                    if (targetDate >= today) continue

                    // 랜덤한 걸음 수 (3000-8000)
                    val stepCount = 10000 + (dayIndex * 100) % 5000

                    // 랜덤한 산책 시간 (15-60분)
                    val durationMillis = (15 + dayIndex % 45) * 60 * 1000L

                    val dummySession = WalkingSession(
                        id = java.util.UUID.randomUUID().toString(), // 명시적으로 UUID 생성
                        userId = userId, // 실제 사용자 ID 사용
                        startTime = targetDate + (8 + dayIndex % 4) * 60 * 60 * 1000L, // 오전 8-11시
                        endTime = targetDate + (8 + dayIndex % 4) * 60 * 60 * 1000L + durationMillis,
                        stepCount = stepCount,
                        locations = emptyList(), // 더미 데이터이므로 GPS 없음
                        filteredLocations = null,
                        smoothedLocations = null,
                        totalDistance = stepCount * 0.0007f, // 걸음 수 기반 거리 (70cm 보폭)
                        preWalkEmotion = emotionTypeToString(EmotionType.entries[dayIndex % EmotionType.entries.size]),
                        postWalkEmotion = emotionTypeToString(EmotionType.entries[(dayIndex + 1) % EmotionType.entries.size]),
                        note = "커스텀 테스트 더미 데이터 ${dayIndex + 1}",
                        localImagePath = null,
                        serverImageUrl = null,
                        createdDate = DateUtils.millisToIsoUtc(targetDate),
                        targetStepCount = 0,
                        targetWalkCount = 0
                    )

                    val savedId = walkingSessionRepository.saveSessionLocalOnly(dummySession)
                    createdCount++
                    Timber.d("✅ 더미 세션 저장됨: ID=$savedId, 날짜=${DateUtils.formatDate(targetDate)}, 걸음=$stepCount")

                    // 너무 빠른 연속 추가 방지
                    if (dayIndex % 10 == 0) {
                        delay(50)
                    }
                }

                Timber.d("✅ CustomTestViewModel: 더미 세션 데이터 추가 완료 ($createdCount/40개)")
                Timber.d("💡 데이터가 표시되지 않는다면:")
                Timber.d("   1. 홈 화면으로 이동")
                Timber.d("   2. 화면을 아래로 당겨서 새로고침")
                Timber.d("   3. 또는 산책 기록 탭으로 이동해서 확인")

                // 샘플 데이터 저장 확인을 위한 로그
                if (createdCount > 0) {
                    Timber.d("📊 저장된 샘플 데이터:")
                    Timber.d("   - 최근 데이터: ${DateUtils.formatDate(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)}")
                    Timber.d("   - 7일 전 데이터: ${DateUtils.formatDate(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)}")
                    Timber.d("   - 30일 전 데이터: ${DateUtils.formatDate(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)}")
                }

                // 저장 완료 후 콜백 실행
                onComplete()

            } catch (e: Exception) {
                Timber.e(e, "❌ CustomTestViewModel: 더미 세션 데이터 추가 실패")
            }
        }
    }
}
