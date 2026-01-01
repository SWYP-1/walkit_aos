package team.swyp.sdu.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

object CalenderUtils {
    fun dayRange(date: LocalDate): Pair<Long, Long> {
        val start = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() - 1
        return start to end
    }

    fun weekRange(date: LocalDate): Pair<Long, Long> {
        val startDate = date.with(DayOfWeek.MONDAY)
        val endDate = startDate.plusDays(7)
        val start = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val end = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() - 1
        return start to end
    }

    fun monthRange(date: LocalDate): Pair<Long, Long> {
        val startDate = date.withDayOfMonth(1)
        val endDate = startDate.plusMonths(1)
        val start = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val end = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() - 1
        return start to end
    }

}