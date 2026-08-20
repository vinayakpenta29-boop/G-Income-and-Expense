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
    // Account Sources CRUD
    @Insert
    void insertSource(AccountSource source);

    @Update
    void updateSource(AccountSource source);

    @Delete
    void deleteSource(AccountSource source);

    @Query("SELECT * FROM source_table ORDER BY id ASC")
    LiveData<List<AccountSource>> getAllSources();

    // Income CRUD
    @Insert
    void insertIncome(Income income);

    @Update
    void updateIncome(Income income);

    @Delete
    void deleteIncome(Income income);

    @Query("SELECT * FROM income_table ORDER BY date DESC")
    LiveData<List<Income>> getAllIncome();

    // Expense CRUD
    @Insert
    void insertExpense(Expense expense);

    @Update
    void updateExpense(Expense expense);

    @Delete
    void deleteExpense(Expense expense);

    @Query("SELECT * FROM expense_table ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses();
}
