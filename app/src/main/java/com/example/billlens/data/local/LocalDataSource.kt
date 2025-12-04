package com.example.billlens.data.local

import com.example.billlens.data.model.Expense
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val expenseDao: ExpenseDao){


    val allVisibleExpenses: Flow<List<Expense>> = expenseDao.getAllVisibleExpenses()

    suspend fun upsertExpense(expense: Expense) {
        expenseDao.upsertExpense(expense)
    }

    suspend fun upsertAll(expenses: List<Expense>) {
        expenseDao.upsertAll(expenses)
    }

    suspend fun getLatestUpdateTimestamp(): Date? = expenseDao.getLatestUpdateTimestamp()

    suspend fun getUnsyncedExpenses(): List<Expense> = expenseDao.getUnsyncedExpenses()
}