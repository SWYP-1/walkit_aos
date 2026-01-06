package team.swyp.sdu.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 계절 타입
 */
enum class Season {
    SPRING,    // 봄
    SUMMER,    // 여름
    AUTUMN,    // 가을
    WINTER     // 겨울
}

data class MonthWeek(
    val month: Int,
    val week: Int,
)

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
            .atZone(ZoneId.of("UTC"))
            .format(isoDateTimeFormatter)
    }
    fun formatDateYearMonthDate(timestampMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern(
            "yyyy년 M월 d일",
            Locale.KOREAN
        )

        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
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

    /**
     * ISO 8601 형식의 날짜+시간 문자열을 한국어 날짜 형식으로 변환
     *
     * @param isoDateTime ISO 8601 형식의 날짜+시간 문자열 (예: "2025-12-28T00:37:57.180871")
     * @return 한국어 형식의 날짜 문자열 (예: "2025년 12월 28일")
     */
    fun formatIsoToKorean(isoDateTime: String): String {
        return try {
            val instant = Instant.parse(isoDateTime)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val koreanFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
            dateTime.format(koreanFormatter)
        } catch (t: Throwable) {
            // 파싱 실패 시 원본 문자열 반환
            isoDateTime
        }
    }

    /**
     * ISO 8601 형식의 날짜+시간 문자열을 간단한 한국어 날짜 형식으로 변환
     *
     * @param isoDateTime ISO 8601 형식의 날짜+시간 문자열 (예: "2025-12-28T00:37:57.180871")
     * @return 한국어 형식의 날짜 문자열 (예: "2025.12.28")
     */
    fun formatIsoToKoreanDate(isoDateTime: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            val dateTime = LocalDateTime.parse(isoDateTime, formatter)
            val koreanFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
            dateTime.format(koreanFormatter)
        } catch (t: Throwable) {
            isoDateTime
        }
    }

    fun getMonthWeek(dateString: String): MonthWeek {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(dateString, formatter)

        val weekFields = WeekFields.of(Locale.KOREA)

        return MonthWeek(
            month = date.monthValue,
            week = date.get(weekFields.weekOfMonth())
        )
    }

    /**
     * 밀리초 타임스탬프를 시:분 형식으로 변환 (예: "12:23")
     *
     * @param timestampMillis 밀리초 타임스탬프
     * @return 시:분 형식의 시간 문자열 (예: "12:23")
     */
    fun formatToTimeHHMM(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    /**
     * 밀리초 타임스탬프를 시:분:초 형식으로 변환 (예: "12:23:45")
     *
     * @param timestampMillis 밀리초 타임스탬프
     * @return 시:분:초 형식의 시간 문자열 (예: "12:23:45")
     */
    fun formatToTimeHHMMSS(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    /**
     * 이번 주 범위 반환 (월요일 00:00:00 ~ 오늘 23:59:59)
     *
     * @return Pair<startMillis, endMillis> 이번 주 월요일부터 오늘까지의 타임스탬프
     */
    fun getCurrentWeekRange(): Pair<Long, Long> {
        val now = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())

        // 이번 주 월요일 찾기
        val monday = now.with(weekFields.dayOfWeek(), 1L) // 1 = 월요일

        // 월요일 00:00:00
        val startOfWeek = monday.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 오늘 23:59:59
        val endOfToday = now
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return Pair(startOfWeek, endOfToday)
    }


}

