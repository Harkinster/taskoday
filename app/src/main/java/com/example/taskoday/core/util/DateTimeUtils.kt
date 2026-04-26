package com.example.taskoday.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateTimeUtils {
    fun todayBoundsMillis(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> {
        val start = startOfDayMillis(nowMillis, zoneId)
        val end = start + DAY_MILLIS - 1L
        return start to end
    }

    fun startOfDayMillis(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val date = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun startOfDayMillis(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun toLocalDate(
        dayStartMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate()

    fun plusDays(
        dayStartMillis: Long,
        days: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = toLocalDate(dayStartMillis, zoneId).plusDays(days).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun dayOfWeekIso(
        dayStartMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int = toLocalDate(dayStartMillis, zoneId).dayOfWeek.value

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null) return "Aucune date"
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.FRENCH)
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null) return "Aucune heure"
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.FRENCH)
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }

    fun formatTimeOnly(epochMillis: Long?): String {
        if (epochMillis == null) return "--:--"
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime())
    }

    fun formatDayLabel(
        dayStartMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
        return formatter.format(toLocalDate(dayStartMillis, zoneId))
    }

    fun currentDateLabel(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = formatDayLabel(startOfDayMillis(nowMillis, zoneId), zoneId)

    fun weekDayLabels(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<String> {
        val start = startOfCurrentWeek(nowMillis, zoneId)
        val formatter = DateTimeFormatter.ofPattern("EEE d", Locale.FRENCH)
        return (0..6).map { offset ->
            formatter.format(toLocalDate(start, zoneId).plusDays(offset.toLong()))
        }
    }

    fun startOfCurrentWeek(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startDate = now.minusDays(now.dayOfWeek.value.toLong() - 1L)
        return startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun epochMillisAtHour(
        date: LocalDate,
        hour: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = date.atTime(hour, 0).atZone(zoneId).toInstant().toEpochMilli()

    fun epochMillisAtTime(
        date: LocalDate,
        time: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()

    private const val DAY_MILLIS: Long = 24L * 60L * 60L * 1000L
}
