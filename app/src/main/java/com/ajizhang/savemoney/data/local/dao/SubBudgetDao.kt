package com.ajizhang.savemoney.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ajizhang.savemoney.data.local.entity.SubBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubBudgetDao {
    @Query("SELECT * FROM sub_budgets WHERE monthlyBudgetId = :monthlyBudgetId ORDER BY createdAt ASC")
    fun observeByMonthlyBudgetId(monthlyBudgetId: Long): Flow<List<SubBudgetEntity>>

    @Query("SELECT * FROM sub_budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SubBudgetEntity?

    @Upsert
    suspend fun upsert(subBudget: SubBudgetEntity)

    @Query("DELETE FROM sub_budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM sub_budgets WHERE monthlyBudgetId = :monthlyBudgetId")
    suspend fun sumByMonthlyBudgetId(monthlyBudgetId: Long): Long
}
