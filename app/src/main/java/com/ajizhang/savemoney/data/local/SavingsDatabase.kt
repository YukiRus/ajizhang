package com.ajizhang.savemoney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ajizhang.savemoney.data.local.dao.CategoryDao
import com.ajizhang.savemoney.data.local.dao.GoalDao
import com.ajizhang.savemoney.data.local.dao.InvestmentDao
import com.ajizhang.savemoney.data.local.dao.TransactionDao
import com.ajizhang.savemoney.data.local.entity.CategoryEntity
import com.ajizhang.savemoney.data.local.entity.GoalEntity
import com.ajizhang.savemoney.data.local.entity.InvestmentEntity
import com.ajizhang.savemoney.data.local.entity.TransactionEntity

@Database(
    entities = [GoalEntity::class, InvestmentEntity::class, TransactionEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun transactionDao(): TransactionDao

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
    }
}
