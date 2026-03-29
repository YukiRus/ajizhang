package com.ajizhang.savemoney.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatter {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayEpochMillis(): Long = localDateToEpochMillis(LocalDate.now())

    fun format(epochMillis: Long): String = epochMillisToLocalDate(epochMillis).format(formatter)

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    fun localDateToEpochMillis(localDate: LocalDate): Long =
        localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
