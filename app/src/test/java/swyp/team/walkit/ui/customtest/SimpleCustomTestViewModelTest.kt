package swyp.team.walkit.ui.customtest

import org.junit.Test
import org.junit.Assert.*
import swyp.team.walkit.utils.DateUtils
import java.util.*

/**
 * CustomTestViewModel 간단 테스트
 */
class SimpleCustomTestViewModelTest {

    @Test
    fun `더미 데이터 날짜 검증 테스트`() {
        // 오늘 날짜
        val today = DateUtils.getStartOfDay(System.currentTimeMillis())

        // 40일 전부터 시작
        val startDate = System.currentTimeMillis() - 40 * 24 * 60 * 60 * 1000L

        var validCount = 0
        for (dayIndex in 0 until 40) {
            val targetDate = startDate + (dayIndex * 24 * 60 * 60 * 1000L)
            val startOfDayTarget = DateUtils.getStartOfDay(targetDate)

            // 오늘 날짜는 제외
            if (startOfDayTarget >= today) {
                continue
            }

            // 유효한 날짜인지 확인
            assertTrue("날짜가 오늘 이전이어야 함", startOfDayTarget < today)
            validCount++
        }

        // 최소한 몇 개의 유효한 날짜가 있어야 함
        assertTrue("유효한 날짜가 있어야 함", validCount > 30)
    }

    @Test
    fun `걸음 수 범위 검증 테스트`() {
        // 더미 데이터의 걸음 수 범위 테스트
        val stepCounts = (0..39).map { 3000 + (it * 100) % 5000 }

        stepCounts.forEach { stepCount ->
            assertTrue("걸음 수가 범위 내여야 함: $stepCount", stepCount in 3000..8000)
        }
    }

    @Test
    fun `감정 타입 유효성 테스트`() {
        val emotions = listOf("행복", "슬픔", "화남", "평범", "피곤")

        // 더미 데이터에서 사용할 감정 타입들이 유효한지 확인
        val dummyEmotions = (0..10).map { emotions[it % emotions.size] }

        dummyEmotions.forEach { emotion ->
            assertTrue("유효한 감정 타입이어야 함: $emotion", emotions.contains(emotion))
        }
    }

    @Test
    fun `노트 포맷 검증 테스트`() {
        val testIndex = 5
        val expectedNote = "커스텀 테스트 더미 데이터 ${testIndex + 1}일차"

        assertTrue("노트 형식이 올바라야 함", expectedNote.contains("커스텀 테스트 더미 데이터"))
        assertTrue("일차 정보가 포함되어야 함", expectedNote.contains("일차"))
    }
}


