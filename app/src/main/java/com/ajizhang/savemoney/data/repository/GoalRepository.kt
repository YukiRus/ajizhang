package com.ajizhang.savemoney.data.repository

import com.ajizhang.savemoney.data.local.dao.GoalDao
import com.ajizhang.savemoney.data.local.entity.GoalEntity
import com.ajizhang.savemoney.data.model.Goal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
) {
    fun observeGoal(): Flow<Goal?> =
        goalDao.observeGoal().map { entity ->
            entity?.let {
                Goal(
                    name = it.name,
                    targetAmount = it.targetAmount,
                )
            }
        }

    suspend fun saveGoal(name: String, targetAmount: Long, updatedAt: Long) {
        goalDao.upsert(
            GoalEntity(
                name = name,
                targetAmount = targetAmount,
                updatedAt = updatedAt,
            ),
        )
    }
}
