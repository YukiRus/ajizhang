package com.ajizhang.savemoney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investment")
data class InvestmentEntity(
    @PrimaryKey val id: Int = 1,
    val currentAmount: Long,
    val updatedAt: Long,
)
