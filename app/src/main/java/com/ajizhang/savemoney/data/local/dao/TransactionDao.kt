package com.ajizhang.savemoney.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ajizhang.savemoney.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS totalIncome,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount - refundedAmount ELSE 0 END), 0) AS totalExpense
        FROM transactions
        """,
    )
    fun observeSummary(): Flow<TransactionSummaryRow>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount - refundedAmount ELSE 0 END), 0)
        FROM transactions WHERE subBudgetId = :subBudgetId
    """)
    suspend fun sumSpentBySubBudgetId(subBudgetId: Long): Long

    @Query("UPDATE transactions SET subBudgetId = NULL WHERE subBudgetId = :subBudgetId")
    suspend fun unlinkSubBudget(subBudgetId: Long)

    @Query("SELECT * FROM transactions WHERE subBudgetId = :subBudgetId ORDER BY occurredAt DESC, id DESC")
    suspend fun getBySubBudgetId(subBudgetId: Long): List<TransactionEntity>

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
