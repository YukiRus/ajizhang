package com.ajizhang.savemoney.data.remote

import com.ajizhang.savemoney.data.model.RecognizedExpenseItem

object ImageExpenseParser {
    fun parseExpenseArray(jsonText: String): List<RecognizedExpenseItem> {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()

        val items = mutableListOf<RecognizedExpenseItem>()
        var pos = 0
        val chars = trimmed.toCharArray()
        val len = chars.size

        skipWhitespace(chars, pos, len).also { pos = it }
        if (pos >= len || chars[pos] != '[') return emptyList()
        pos++

        while (pos < len) {
            skipWhitespace(chars, pos, len).also { pos = it }
            if (pos >= len) break
            if (chars[pos] == ']') break
            if (chars[pos] == ',') { pos++; continue }

            if (chars[pos] == '{') {
                parseObject(chars, pos, len)?.let { (item, newPos) ->
                    items.add(item)
                    pos = newPos
                } ?: break
            } else {
                pos++
            }
        }

        return items
    }

    fun stripCodeFence(text: String): String =
        text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private fun parseObject(
        chars: CharArray,
        start: Int,
        len: Int,
    ): Pair<RecognizedExpenseItem, Int>? {
        var pos = start + 1 // skip '{'
        var amountText = ""
        var category = ""
        var note = ""
        var dateText = ""
        var timeText = ""
        var budgetSubName = ""

        while (pos < len) {
            skipWhitespace(chars, pos, len).also { pos = it }
            if (pos >= len) return null
            if (chars[pos] == '}') return RecognizedExpenseItem(
                amountText = amountText,
                category = category,
                note = note,
                dateText = dateText,
                timeText = timeText,
                budgetSubName = budgetSubName,
            ) to (pos + 1)

            if (chars[pos] == '"') {
                val keyStart = pos + 1
                val keyEnd = findStringEnd(chars, keyStart, len) ?: return null
                val key = String(chars, keyStart, keyEnd - keyStart)
                pos = keyEnd + 1

                skipWhitespace(chars, pos, len).also { pos = it }
                if (pos >= len || chars[pos] != ':') return null
                pos++

                skipWhitespace(chars, pos, len).also { pos = it }
                if (pos >= len) return null

                if (chars[pos] == '"') {
                    val valStart = pos + 1
                    val valEnd = findStringEnd(chars, valStart, len) ?: return null
                    val value = String(chars, valStart, valEnd - valStart)
                    pos = valEnd + 1

                    when (key) {
                        "amount" -> amountText = value
                        "category" -> category = value
                        "note" -> note = value
                        "date" -> dateText = value
                        "time" -> timeText = value
                        "budgetSubName" -> budgetSubName = value
                    }
                } else {
                    skipValue(chars, pos, len).also { pos = it }
                }
            } else {
                pos++
            }
        }

        return RecognizedExpenseItem(
            amountText = amountText,
            category = category,
            note = note,
            dateText = dateText,
            timeText = timeText,
            budgetSubName = budgetSubName,
        ) to pos
    }

    private fun findStringEnd(chars: CharArray, start: Int, len: Int): Int? {
        var pos = start
        while (pos < len) {
            if (chars[pos] == '\\') {
                pos += 2 // skip escaped char
            } else if (chars[pos] == '"') {
                return pos
            } else {
                pos++
            }
        }
        return null
    }

    private fun skipValue(chars: CharArray, pos: Int, len: Int): Int {
        var i = pos
        while (i < len && chars[i] != ',' && chars[i] != '}' && chars[i] != ']') {
            i++
        }
        return i
    }

    private fun skipWhitespace(chars: CharArray, pos: Int, len: Int): Int {
        var i = pos
        while (i < len && chars[i].isWhitespace()) i++
        return i
    }
}
