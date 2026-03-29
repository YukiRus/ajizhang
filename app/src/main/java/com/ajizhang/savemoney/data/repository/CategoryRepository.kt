package com.ajizhang.savemoney.data.repository

import com.ajizhang.savemoney.data.local.dao.CategoryDao
import com.ajizhang.savemoney.data.local.entity.CategoryEntity
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.CategoryCatalog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeAllCategories(): Flow<Map<TransactionType, List<String>>> =
        categoryDao.observeAllCategories().map { categories ->
            categories
                .groupBy { it.type }
                .mapValues { (_, items) -> items.map(CategoryEntity::name) }
                .let { grouped ->
                    TransactionType.entries.associateWith { type -> grouped[type].orEmpty() }
                }
        }

    suspend fun ensureDefaults() {
        seedIfEmpty(TransactionType.INCOME, CategoryCatalog.incomeDefaults)
        seedIfEmpty(TransactionType.EXPENSE, CategoryCatalog.expenseDefaults)
    }

    suspend fun addCategory(type: TransactionType, name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) {
            return
        }

        val sortOrder = categoryDao.countByType(type.name)
        categoryDao.insert(
            CategoryEntity(
                type = type,
                name = normalized,
                sortOrder = sortOrder,
            ),
        )
    }

    suspend fun deleteCategory(type: TransactionType, name: String) {
        categoryDao.deleteByTypeAndName(type.name, name)
    }

    private suspend fun seedIfEmpty(type: TransactionType, defaults: List<String>) {
        if (categoryDao.countByType(type.name) > 0) {
            return
        }

        defaults.forEachIndexed { index, name ->
            categoryDao.insert(
                CategoryEntity(
                    type = type,
                    name = name,
                    sortOrder = index,
                ),
            )
        }
    }
}
