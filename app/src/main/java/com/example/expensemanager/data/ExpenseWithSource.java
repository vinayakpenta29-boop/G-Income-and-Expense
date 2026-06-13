package com.example.expensemanager.data;

import androidx.room.Embedded;

public class ExpenseWithSource {
    @Embedded
    public Expense expense;
    public String sourceName; 
}
