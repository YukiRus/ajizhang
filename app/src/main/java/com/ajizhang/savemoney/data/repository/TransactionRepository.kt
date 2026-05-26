package com.ajizhang.savemoney.data.repository

import com.ajizhang.savemoney.data.local.dao.TransactionDao
import com.ajizhang.savemoney.data.local.dao.TransactionSummaryRow
import com.ajizhang.savemoney.data.local.entity.TransactionEntity
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) {
    fun observeTransactions(): Flow<List<TransactionRecord>> =
        transactionDao.observeTransactions().map { list ->
            list.map { it.toModel() }
        }

    fun observeSummary(): Flow<TransactionSummaryRow> = transactionDao.observeSummary()

    suspend fun getTransaction(id: Long): TransactionRecord? = transactionDao.getTransactionById(id)?.toModel()

    suspend fun saveTransaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        refundedAmount: Long,
        category: String,
        note: String,
        occurredAt: Long,
        createdAt: Long,
        updatedAt: Long,
        subBudgetId: Long? = null,
    ) {
        transactionDao.upsert(
            TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                refundedAmount = refundedAmount,
                category = category,
                note = note,
                occurredAt = occurredAt,
                createdAt = createdAt,
                updatedAt = updatedAt,
                subBudgetId = subBudgetId,
            ),
        )
    }

    suspend fun deleteTransaction(id: Long) {
        val transaction = transactionDao.getTransactionById(id) ?: return
        transactionDao.delete(transaction)
    }

    suspend fun getTransactionsBySubBudgetId(subBudgetId: Long): List<TransactionRecord> =
        transactionDao.getBySubBudgetId(subBudgetId).map { it.toModel() }

    suspend fun sumSpentBySubBudgetId(subBudgetId: Long): Long =
        transactionDao.sumSpentBySubBudgetId(subBudgetId)

    suspend fun unlinkSubBudget(subBudgetId: Long) {
        transactionDao.unlinkSubBudget(subBudgetId)
    }

    private fun TransactionEntity.toModel(): TransactionRecord =
        TransactionRecord(
            id = id,
            type = type,
            amount = amount,
            refundedAmount = refundedAmount,
            category = category,
            note = note,
            occurredAt = occurredAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            subBudgetId = subBudgetId,
        )
}
