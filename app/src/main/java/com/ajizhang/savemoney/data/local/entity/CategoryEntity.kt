package com.ajizhang.savemoney.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ajizhang.savemoney.data.model.TransactionType

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type", "name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val name: String,
    val sortOrder: Int,
)
