package com.ajizhang.savemoney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ajizhang.savemoney.data.local.dao.BudgetDao
import com.ajizhang.savemoney.data.local.dao.CategoryDao
import com.ajizhang.savemoney.data.local.dao.GoalDao
import com.ajizhang.savemoney.data.local.dao.InvestmentDao
import com.ajizhang.savemoney.data.local.dao.SubBudgetDao
import com.ajizhang.savemoney.data.local.dao.TransactionDao
import com.ajizhang.savemoney.data.local.entity.CategoryEntity
import com.ajizhang.savemoney.data.local.entity.GoalEntity
import com.ajizhang.savemoney.data.local.entity.InvestmentEntity
import com.ajizhang.savemoney.data.local.entity.MonthlyBudgetEntity
import com.ajizhang.savemoney.data.local.entity.SubBudgetEntity
import com.ajizhang.savemoney.data.local.entity.TransactionEntity

@Database(
    entities = [
        GoalEntity::class,
        InvestmentEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        SubBudgetEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun subBudgetDao(): SubBudgetDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS categories (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            type TEXT NOT NULL,
                            name TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_type_name ON categories(type, name)",
                    )
                }
            }

        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE transactions ADD COLUMN refundedAmount INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE goal ADD COLUMN expectedDate INTEGER",
                    )
                }
            }

        val MIGRATION_4_5: Migration =
            object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS monthly_budgets (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            monthKey TEXT NOT NULL,
                            totalAmount INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_budgets_monthKey ON monthly_budgets(monthKey)",
                    )
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sub_budgets (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            monthlyBudgetId INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            amount INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            FOREIGN KEY (monthlyBudgetId) REFERENCES monthly_budgets(id) ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_sub_budgets_monthlyBudgetId ON sub_budgets(monthlyBudgetId)",
                    )
                    database.execSQL(
                        "ALTER TABLE transactions ADD COLUMN subBudgetId INTEGER",
                    )
                }
            }
    }
}
