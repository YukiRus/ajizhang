package com.ajizhang.savemoney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val targetAmount: Long,
    val updatedAt: Long,
)
