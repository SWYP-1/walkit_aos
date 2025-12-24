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
fun formatDuration(millis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}





