package com.ajizhang.savemoney.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ajizhang.savemoney.data.local.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getByMonthKey(monthKey: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    fun observeByMonthKey(monthKey: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets ORDER BY monthKey DESC")
    fun observeAll(): Flow<List<MonthlyBudgetEntity>>

    @Upsert
    suspend fun upsert(budget: MonthlyBudgetEntity)

    @Delete
    suspend fun delete(budget: MonthlyBudgetEntity)
}
