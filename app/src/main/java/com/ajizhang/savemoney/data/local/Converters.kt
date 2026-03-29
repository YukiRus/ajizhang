package com.ajizhang.savemoney.data.local

import androidx.room.TypeConverter
import com.ajizhang.savemoney.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
