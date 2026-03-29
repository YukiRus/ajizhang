package com.ajizhang.savemoney.di

import android.content.Context
import androidx.room.Room
import com.ajizhang.savemoney.data.local.SavingsDatabase
import com.ajizhang.savemoney.data.local.dao.CategoryDao
import com.ajizhang.savemoney.data.local.dao.GoalDao
import com.ajizhang.savemoney.data.local.dao.InvestmentDao
import com.ajizhang.savemoney.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SavingsDatabase =
        Room.databaseBuilder(
            context,
            SavingsDatabase::class.java,
            "savings.db",
        ).addMigrations(
            SavingsDatabase.MIGRATION_1_2,
            SavingsDatabase.MIGRATION_2_3,
            SavingsDatabase.MIGRATION_3_4,
        ).build()

    @Provides
    fun provideCategoryDao(database: SavingsDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideGoalDao(database: SavingsDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideInvestmentDao(database: SavingsDatabase): InvestmentDao = database.investmentDao()

    @Provides
    fun provideTransactionDao(database: SavingsDatabase): TransactionDao = database.transactionDao()
}
