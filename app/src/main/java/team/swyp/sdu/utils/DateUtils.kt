package team.swyp.sdu.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 계절 타입
 */
enum class Season {
    SPRING,    // 봄
    SUMMER,    // 여름
    AUTUMN,    // 가을
    WINTER     // 겨울
}

/**
 * 날짜/시간 유틸리티 함수
 */
object DateUtils {
    /**
     * ISO 8601 형식의 날짜+시간 포맷터 (예: "2024-01-01T12:00:00")
     */
    private val isoDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault())

    /**
     * 밀리초 타임스탬프를 ISO 8601 형식의 날짜 문자열로 변환
     *
     * @param timestampMillis 밀리초 타임스탬프
     * @return ISO 8601 형식의 날짜 문자열 (예: "2024-01-01T12:00:00")
     */
    fun formatToIsoDateTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(isoDateTimeFormatter)
    }

    /**
     * 밀리초 타임스탬프를 ISO 8601 UTC 형식의 문자열로 변환
     *
     * @param millis 밀리초 타임스탬프
     * @return ISO 8601 UTC 형식의 문자열 (예: "2024-01-01T12:00:00Z")
     */
    fun millisToIsoUtc(millis: Long): String {
        return Instant.ofEpochMilli(millis).toString()
    }

    /**
     * 오늘 날짜를 기준으로 계절을 판단
     *
     * 봄: 3월 ~ 5월
     * 여름: 6월 ~ 8월
     * 가을: 9월 ~ 11월
     * 겨울: 12월 ~ 2월
     *
     * @return 현재 계절
     */
    fun getCurrentSeason(): Season {
        val today = LocalDate.now()
        val month = today.monthValue

        return when (month) {
            3, 4, 5 -> Season.SPRING    // 봄
            6, 7, 8 -> Season.SUMMER    // 여름
            9, 10, 11 -> Season.AUTUMN  // 가을
            12, 1, 2 -> Season.WINTER   // 겨울
            else -> Season.SPRING       // 기본값 (실제로 발생하지 않음)
        }
    }

    /**
     * 지정된 날짜를 기준으로 계절을 판단
     *
     * 봄: 3월 ~ 5월
     * 여름: 6월 ~ 8월
     * 가을: 9월 ~ 11월
     * 겨울: 12월 ~ 2월
     *
     * @param date 기준 날짜
     * @return 해당 날짜의 계절
     */
    fun getSeason(date: LocalDate): Season {
        val month = date.monthValue

        return when (month) {
            3, 4, 5 -> Season.SPRING    // 봄
            6, 7, 8 -> Season.SUMMER    // 여름
            9, 10, 11 -> Season.AUTUMN  // 가을
            12, 1, 2 -> Season.WINTER   // 겨울
            else -> Season.SPRING       // 기본값 (실제로 발생하지 않음)
        }
    }
}

