package com.example.taskoday.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateTimeUtils {
    fun todayBoundsMillis(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return start to end
    }

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null) return "Aucune date d’échéance"
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.FRENCH)
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null) return "Aucun rappel"
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.FRENCH)
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }

    fun currentDateLabel(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)
        val date = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return formatter.format(date)
    }

    fun startOfCurrentWeek(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val weekStartDate = now.toLocalDate().minusDays(now.dayOfWeek.value.toLong() - 1L)
        return weekStartDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun weekDayLabels(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<String> {
        val start = Instant.ofEpochMilli(startOfCurrentWeek(nowMillis, zoneId)).atZone(zoneId).toLocalDate()
        return (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            val formatter = DateTimeFormatter.ofPattern("EEE d", Locale.FRENCH)
            formatter.format(date)
        }
    }

    fun epochMillisAtHour(
        date: LocalDate,
        hour: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = date.atTime(hour, 0).atZone(zoneId).toInstant().toEpochMilli()
}
