package com.ajizhang.savemoney.data.repository

import com.ajizhang.savemoney.data.local.dao.BudgetDao
import com.ajizhang.savemoney.data.local.dao.SubBudgetDao
import com.ajizhang.savemoney.data.local.entity.MonthlyBudgetEntity
import com.ajizhang.savemoney.data.local.entity.SubBudgetEntity
import com.ajizhang.savemoney.data.model.MonthlyBudget
import com.ajizhang.savemoney.data.model.SubBudget
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val subBudgetDao: SubBudgetDao,
    private val transactionRepository: TransactionRepository,
) {
    fun observeBudgetByMonth(monthKey: String): Flow<MonthlyBudget?> =
        budgetDao.observeByMonthKey(monthKey).map { it?.toModel() }

    fun observeSubBudgets(monthlyBudgetId: Long): Flow<List<SubBudget>> =
        combine(
            subBudgetDao.observeByMonthlyBudgetId(monthlyBudgetId),
            transactionRepository.observeTransactions(),
        ) { entities, transactions ->
            entities.map { entity ->
                val spent = transactions
                    .filter { it.subBudgetId == entity.id }
                    .sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0) }
                entity.toModel(spent)
            }
        }

    fun observeAllBudgets(): Flow<List<MonthlyBudget>> =
        budgetDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getBudgetByMonth(monthKey: String): MonthlyBudget? =
        budgetDao.getByMonthKey(monthKey)?.toModel()

    suspend fun saveBudget(monthKey: String, totalAmount: Long) {
        val now = System.currentTimeMillis()
        val existing = budgetDao.getByMonthKey(monthKey)
        budgetDao.upsert(
            MonthlyBudgetEntity(
                id = existing?.id ?: 0,
                monthKey = monthKey,
                totalAmount = totalAmount,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    suspend fun addSubBudget(monthlyBudgetId: Long, name: String, amount: Long) {
        subBudgetDao.upsert(
            SubBudgetEntity(
                monthlyBudgetId = monthlyBudgetId,
                name = name.trim(),
                amount = amount,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateSubBudget(id: Long, name: String, amount: Long) {
        val existing = subBudgetDao.getById(id) ?: return
        subBudgetDao.upsert(
            existing.copy(name = name.trim(), amount = amount),
        )
    }

    suspend fun deleteSubBudget(id: Long) {
        transactionRepository.unlinkSubBudget(id)
        subBudgetDao.deleteById(id)
    }

    suspend fun getSubBudgetSum(monthlyBudgetId: Long): Long =
        subBudgetDao.sumByMonthlyBudgetId(monthlyBudgetId)

    suspend fun copySubBudgets(sourceMonthKey: String, targetMonthKey: String) {
        val sourceBudget = budgetDao.getByMonthKey(sourceMonthKey) ?: return
        val targetBudget = budgetDao.getByMonthKey(targetMonthKey)
        val now = System.currentTimeMillis()

        val targetId = if (targetBudget != null) {
            targetBudget.id
        } else {
            val newBudget = MonthlyBudgetEntity(
                monthKey = targetMonthKey,
                totalAmount = sourceBudget.totalAmount,
                createdAt = now,
                updatedAt = now,
            )
            budgetDao.upsert(newBudget)
            budgetDao.getByMonthKey(targetMonthKey)?.id ?: return
        }

        val entities = subBudgetDao.observeByMonthlyBudgetId(sourceBudget.id).first()
        for (entity in entities) {
            subBudgetDao.upsert(
                SubBudgetEntity(
                    monthlyBudgetId = targetId,
                    name = entity.name,
                    amount = entity.amount,
                    createdAt = now,
                ),
            )
        }
    }

    private fun MonthlyBudgetEntity.toModel(): MonthlyBudget =
        MonthlyBudget(
            id = id,
            monthKey = monthKey,
            totalAmount = totalAmount,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun SubBudgetEntity.toModel(spent: Long = 0): SubBudget =
        SubBudget(
            id = id,
            monthlyBudgetId = monthlyBudgetId,
            name = name,
            amount = amount,
            spent = spent,
            createdAt = createdAt,
        )
}
