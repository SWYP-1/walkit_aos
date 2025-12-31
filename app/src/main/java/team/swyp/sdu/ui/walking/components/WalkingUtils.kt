package team.swyp.sdu.ui.walking.components

import team.swyp.sdu.utils.FormatUtils
import java.util.concurrent.TimeUnit

/**
 * 거리 포맷팅 유틸리티 (FormatUtils로 통합됨 - 하위 호환성 유지)
 */
fun formatDistance(meters: Float): String = FormatUtils.formatDistanceWalkingUtils(meters)

/**
 * 시간 포맷팅 유틸리티 (FormatUtils로 통합됨 - 하위 호환성 유지)
 */
fun formatToMinutesSeconds(millis: Long): String = FormatUtils.formatToMinutesSeconds(millis)

fun formatToHoursMinutesSeconds(millis: Long): String = FormatUtils.formatToHoursMinutesSeconds(millis)










