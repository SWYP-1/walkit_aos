package team.swyp.sdu.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

object CalenderUtils {
    /**
     * 날짜 범위 계산 (시스템 시간대 사용)
     * 
     * startTime은 System.currentTimeMillis()로 저장되므로 시스템 시간대를 사용해야 합니다.
     */
    fun dayRange(date: LocalDate): Pair<Long, Long> {
        val systemZone = ZoneId.systemDefault()
        val start = date.atStartOfDay(systemZone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(systemZone).toInstant().toEpochMilli() - 1
        return start to end
    }

    fun weekRange(date: LocalDate): Pair<Long, Long> {
        val systemZone = ZoneId.systemDefault()
        val startDate = date.with(DayOfWeek.MONDAY)
        val endDate = startDate.plusDays(7)
        val start = startDate.atStartOfDay(systemZone).toInstant().toEpochMilli()
        val end = endDate.atStartOfDay(systemZone).toInstant().toEpochMilli() - 1
        return start to end
    }

    fun monthRange(date: LocalDate): Pair<Long, Long> {
        val systemZone = ZoneId.systemDefault()
        val startDate = date.withDayOfMonth(1)
        val endDate = startDate.plusMonths(1)
        val start = startDate.atStartOfDay(systemZone).toInstant().toEpochMilli()
        val end = endDate.atStartOfDay(systemZone).toInstant().toEpochMilli() - 1
        return start to end
    }

}