package com.example.expensemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExpenseManagerDao {
    @Insert
    void insertIncome(Income income);

    @Insert
    void insertExpense(Expense expense);

    // NEW: Update targets matching primary keys
    @Update
    void updateIncome(Income income);

    @Update
    void updateExpense(Expense expense);

    // NEW: Delete targets matching primary keys
    @Delete
    void deleteIncome(Income income);

    @Delete
    void deleteExpense(Expense expense);

    @Query("SELECT * FROM income_table ORDER BY date DESC")
    LiveData<List<Income>> getAllIncome();

    @Query("SELECT expense_table.*, income_table.source AS sourceName " +
           "FROM expense_table " +
           "LEFT JOIN income_table ON expense_table.incomeSourceId = income_table.id " +
           "ORDER BY expense_table.date DESC")
    LiveData<List<ExpenseWithSource>> getAllExpensesWithSource();
}
