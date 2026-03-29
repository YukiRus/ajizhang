package com.ajizhang.savemoney.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyFormatter {
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.CHINA).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    fun format(amountInCents: Long): String {
        val absoluteAmount = BigDecimal(amountInCents).abs().divide(BigDecimal(100))
        val prefix = if (amountInCents < 0) "-" else ""
        return prefix + currencyFormat.format(absoluteAmount)
    }

    fun parseToCents(text: String): Long? {
        val normalized = text.trim().replace(",", "")
        if (normalized.isBlank()) {
            return null
        }

        val amount = normalized.toBigDecimalOrNull() ?: return null
        if (amount < BigDecimal.ZERO) {
            return null
        }

        return amount
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun toInputValue(amountInCents: Long): String =
        BigDecimal(amountInCents)
            .divide(BigDecimal(100))
            .stripTrailingZeros()
            .toPlainString()
}
