package team.swyp.sdu.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
}

