package com.example.expensemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ExpenseManagerDao {
    @Insert
    void insertIncome(Income income);

    @Insert
    void insertExpense(Expense expense);

    @Query("SELECT * FROM income_table ORDER BY date DESC")
    LiveData<List<Income>> getAllIncome();

    @Query("SELECT expense_table.*, income_table.source AS sourceName " +
           "FROM expense_table " +
           "LEFT JOIN income_table ON expense_table.incomeSourceId = income_table.id " +
           "ORDER BY expense_table.date DESC")
    LiveData<List<ExpenseWithSource>> getAllExpensesWithSource();
}
