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
    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end AND currency = :currency ORDER BY date DESC, id DESC")
    fun observeExpenses(start: LocalDate, end: LocalDate, currency: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findExpense(id: Long): ExpenseEntity?

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("SELECT * FROM budgets WHERE periodType = 'MONTH' AND periodStart BETWEEN :start AND :end AND currency = :currency")
    fun observeBudgets(start: LocalDate, end: LocalDate, currency: String): Flow<List<BudgetEntity>>

    // Prefer the shared setting, falling back to the most recently edited legacy daily budget.
    @Query("SELECT * FROM budgets WHERE periodType IN ('DAY', 'DAILY') AND currency = :currency ORDER BY CASE WHEN periodType = 'DAILY' THEN 0 ELSE 1 END, updatedAt DESC, periodStart DESC LIMIT 1")
    fun observeDailyBudget(currency: String): Flow<BudgetEntity?>

    @Upsert
    suspend fun saveBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE periodStart = :start AND periodType = :type AND currency = :currency")
    suspend fun deleteBudget(start: LocalDate, type: String, currency: String)

    @Query("DELETE FROM budgets WHERE periodType IN ('DAY', 'DAILY') AND currency = :currency")
    suspend fun deleteDailyBudget(currency: String)
}
