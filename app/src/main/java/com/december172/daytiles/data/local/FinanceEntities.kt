package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "expenses", indices = [Index(value = ["date", "currency"])])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val category: String = "other",
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "budgets", primaryKeys = ["periodStart", "periodType", "currency"])
data class BudgetEntity(
    val periodStart: LocalDate,
    val periodType: String,
    val currency: String,
    val amountMinor: Long,
    val updatedAt: Long,
)
