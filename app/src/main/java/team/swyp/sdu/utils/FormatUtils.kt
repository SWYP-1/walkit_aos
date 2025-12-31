package team.swyp.sdu.utils

import kotlin.math.roundToInt

/**
 * 통합된 포맷팅 유틸리티 클래스
 * 프로젝트 전반의 format 함수들을 중앙 집중화하여 일관성과 재사용성을 높입니다.
 */
object FormatUtils {

    /**
     * 시간 포맷팅 스타일
     */
    enum class DurationStyle {
        /** "X시간 Y분" 형식 (WalkingSummaryCard 스타일) */
        HOURS_MINUTES,

        /** "MM:SS" 형식 (HomeCard, TestWalkingScreen 스타일) */
        MINUTES_SECONDS,

        /** "HH:MM:SS" 형식 (WalkingUtils 스타일) */
        HOURS_MINUTES_SECONDS,

        /** "X시간 Y분" 또는 "Y분" (RecordComponents 스타일) */
        AUTO_HOURS_MINUTES
    }

    /**
     * 거리 포맷팅 스타일
     */
    enum class DistanceStyle {
        /** 자동 선택 (1000m 이상: km, 미만: m) */
        AUTO,

        /** 항상 km 단위 */
        KILOMETERS,

        /** 항상 m 단위 */
        METERS,

        /** 값과 단위 분리 (Pair 반환) */
        SEPARATED
    }

    /**
     * 시간 포맷팅 (통합)
     *
     * @param durationMillis 밀리초 단위 시간
     * @param style 포맷팅 스타일
     * @return 포맷팅된 시간 문자열
     */
    fun formatDuration(durationMillis: Long, style: DurationStyle = DurationStyle.AUTO_HOURS_MINUTES): String {
        return when (style) {
            DurationStyle.HOURS_MINUTES -> formatDurationHoursMinutes(durationMillis)
            DurationStyle.MINUTES_SECONDS -> formatDurationMinutesSeconds(durationMillis)
            DurationStyle.HOURS_MINUTES_SECONDS -> formatDurationHoursMinutesSeconds(durationMillis)
            DurationStyle.AUTO_HOURS_MINUTES -> formatDurationAutoHoursMinutes(durationMillis)
        }
    }

    /**
     * 시간 포맷팅: "X시간 Y분" 형식 (WalkingSummaryCard 스타일)
     */
    private fun formatDurationHoursMinutes(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${hours}시간 ${minutes}분"  // 통일성을 위해 0시간도 표시
            else -> "0시간 0분"
        }
    }

    /**
     * 시간 포맷팅: "MM:SS" 형식 (HomeCard, TestWalkingScreen 스타일)
     */
    private fun formatDurationMinutesSeconds(durationMillis: Long): String {
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(durationMillis)
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    /**
     * 시간 포맷팅: "HH:MM:SS" 형식 (WalkingUtils 스타일)
     */
    private fun formatDurationHoursMinutesSeconds(durationMillis: Long): String {
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(durationMillis)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    /**
     * 시간 포맷팅: "X시간 Y분" 또는 "Y분" (RecordComponents 스타일)
     */
    internal fun formatDurationAutoHoursMinutes(durationMillis: Long): String {
        val totalMinutes = durationMillis / 1000 / 60
        return if (totalMinutes < 60) {
            // 1시간 미만이면 분으로만 표시
            "${totalMinutes}분"
        } else {
            // 1시간 이상이면 시간과 분으로 표시
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes > 0) {
                "${hours}시간 ${minutes}분"
            } else {
                "${hours}시간"
            }
        }
    }

    /**
     * 거리 포맷팅 (통합)
     *
     * @param meters 미터 단위 거리
     * @param style 포맷팅 스타일
     * @return 포맷팅된 거리 (String 또는 Pair에 따라 다름)
     */
    fun formatDistance(meters: Float, style: DistanceStyle = DistanceStyle.AUTO): Any {
        return when (style) {
            DistanceStyle.AUTO -> formatDistanceAuto(meters)
            DistanceStyle.KILOMETERS -> formatDistanceKilometers(meters)
            DistanceStyle.METERS -> formatDistanceMeters(meters)
            DistanceStyle.SEPARATED -> formatDistanceSeparated(meters)
        }
    }

    /**
     * 거리 포맷팅: 자동 선택 (WalkingSummaryCard 스타일)
     * 1000m 이상: 소수점 1자리 km, 미만: 천 단위 구분자 m
     */
    private fun formatDistanceAuto(meters: Float): String {
        return if (meters >= 1000) {
            String.format("%.1f", meters / 1000f) + "km"
        } else {
            NumberUtils.formatNumber(meters.roundToInt()) + "m"
        }
    }

    /**
     * 거리 포맷팅: 항상 km 단위 (TestWalkingScreen, WalkingUtils 스타일)
     */
    private fun formatDistanceKilometers(meters: Float): String {
        return String.format("%.2f", meters / 1000f) + "km"
    }

    /**
     * 거리 포맷팅: 항상 m 단위
     */
    private fun formatDistanceMeters(meters: Float): String {
        return "${meters.roundToInt()}m"
    }

    /**
     * 거리 포맷팅: 값과 단위 분리 (HomeCard 스타일)
     */
    private fun formatDistanceSeparated(meters: Float): Pair<String, String> {
        return if (meters >= 1000f) {
            Pair(String.format("%.2f", meters / 1000f), "km")
        } else {
            Pair(NumberUtils.formatNumber(meters.roundToInt()), "m")
        }
    }

    /**
     * 걸음 수 포맷팅 (WalkingSummaryCard에서 사용)
     * 천 단위 구분자 적용
     */
    fun formatStepCount(stepCount: Int): String {
        return NumberUtils.formatNumber(stepCount)
    }

    /**
     * 기존 함수들과의 호환성을 위한 함수들
     */

    // WalkingSummaryCard.kt에서 사용하던 함수들
    fun formatDurationCompat(durationMs: Long): String = formatDuration(durationMs, DurationStyle.HOURS_MINUTES)
    fun formatDistanceCompat(distanceMeters: Float): String = formatDistance(distanceMeters, DistanceStyle.AUTO) as String
    fun formatStepCountCompat(stepCount: Int): String = formatStepCount(stepCount)

    // HomeCard.kt에서 사용하던 함수들
    fun formatDurationMMSS(durationMillis: Long): String = formatDuration(durationMillis, DurationStyle.MINUTES_SECONDS)
    fun formatDate(timestampMillis: Long): String = DateUtils.formatToIsoDateTime(timestampMillis).substring(0, 10) // YYYY-MM-DD 형식

    // WalkingUtils.kt에서 사용하던 함수들
    fun formatToMinutesSeconds(millis: Long): String = formatDuration(millis, DurationStyle.MINUTES_SECONDS)
    fun formatToHoursMinutesSeconds(millis: Long): String = formatDuration(millis, DurationStyle.HOURS_MINUTES_SECONDS)
    fun formatDistanceWalkingUtils(meters: Float): String = formatDistance(meters, DistanceStyle.KILOMETERS) as String

    // RecordComponents.kt에서 사용하던 함수
    fun formatWalkingTime(totalMinutes: Long): String = formatDurationAutoHoursMinutes(totalMinutes * 60 * 1000L)

    // TestWalkingScreen.kt에서 사용하던 함수들
    fun formatDistanceTest(meters: Float): String = formatDistance(meters, DistanceStyle.KILOMETERS) as String
}