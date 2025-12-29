package team.swyp.sdu.ui.walking.components

import java.util.concurrent.TimeUnit

/**
 * 거리 포맷팅 유틸리티
 */
fun formatDistance(meters: Float): String =
    if (meters >= 1000f) {
        String.format("%.2f km", meters / 1000f)
    } else {
        String.format("%.0f m", meters)
    }

/**
 * 시간 포맷팅 유틸리티
 */
fun formatToMinutesSeconds(millis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
fun formatToHoursMinutesSeconds(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return String.format(
        "%02d:%02d:%02d",
        hours,
        minutes,
        seconds,
    )
}









