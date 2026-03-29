package com.ajizhang.savemoney.data.repository

import com.ajizhang.savemoney.data.local.dao.InvestmentDao
import com.ajizhang.savemoney.data.local.entity.InvestmentEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class InvestmentRepository @Inject constructor(
    private val investmentDao: InvestmentDao,
) {
    fun observeInvestmentAmount(): Flow<Long> =
        investmentDao.observeInvestment().map { it?.currentAmount ?: 0L }

    suspend fun ensureInitialized(currentTimeMillis: Long) {
        if (investmentDao.getInvestment() == null) {
            investmentDao.upsert(
                InvestmentEntity(
                    currentAmount = 0L,
                    updatedAt = currentTimeMillis,
                ),
            )
        }
    }

    suspend fun saveInvestmentAmount(amount: Long, updatedAt: Long) {
        investmentDao.upsert(
            InvestmentEntity(
                currentAmount = amount,
                updatedAt = updatedAt,
            ),
        )
    }
}
