package com.ajizhang.savemoney.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ajizhang.savemoney.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investment WHERE id = 1")
    fun observeInvestment(): Flow<InvestmentEntity?>

    @Query("SELECT * FROM investment WHERE id = 1")
    suspend fun getInvestment(): InvestmentEntity?

    @Upsert
    suspend fun upsert(investment: InvestmentEntity)
}
