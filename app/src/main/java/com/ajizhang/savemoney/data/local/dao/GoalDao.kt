package com.ajizhang.savemoney.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ajizhang.savemoney.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal WHERE id = 1")
    fun observeGoal(): Flow<GoalEntity?>

    @Upsert
    suspend fun upsert(goal: GoalEntity)
}
