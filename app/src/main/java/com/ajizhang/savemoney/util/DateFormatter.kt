package com.ajizhang.savemoney.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatter {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val monthKeyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun currentMonthKey(): String = LocalDate.now().format(monthKeyFormatter)

    fun monthKeyToLocalDate(monthKey: String): LocalDate =
        java.time.YearMonth.parse(monthKey, monthKeyFormatter).atDay(1)

    fun todayEpochMillis(): Long = localDateToEpochMillis(LocalDate.now())

    fun format(epochMillis: Long): String = epochMillisToLocalDate(epochMillis).format(formatter)

    fun formatWithOptionalTime(epochMillis: Long): String {
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        return if (dateTime.toLocalTime() == LocalTime.MIDNIGHT) {
            dateTime.toLocalDate().format(formatter)
        } else {
            dateTime.format(dateTimeFormatter)
        }
    }

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    fun localDateToEpochMillis(localDate: LocalDate): Long =
        localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun localDateTimeToEpochMillis(localDateTime: LocalDateTime): Long =
        localDateTime.atZone(zoneId).toInstant().toEpochMilli()
}
