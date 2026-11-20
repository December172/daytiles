package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeExpenses(start: LocalDate, end: LocalDate): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findExpense(id: Long): ExpenseEntity?

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("""
        SELECT * FROM budgets
        WHERE (periodType IN ('SHARED_DAILY', 'DAILY', 'DAY'))
           OR (periodType IN ('SHARED_MONTH', 'MONTH') AND periodStart = :monthStart)
        ORDER BY CASE WHEN periodType = 'DAY' THEN 1 ELSE 0 END,
            updatedAt DESC, periodStart DESC, currency
    """)
    fun observeBudgetRows(monthStart: LocalDate): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun saveBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE periodStart = :start AND periodType = :type")
    suspend fun deleteSharedBudget(start: LocalDate, type: String)
}
