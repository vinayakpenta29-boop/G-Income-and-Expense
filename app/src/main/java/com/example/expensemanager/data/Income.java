package com.example.expensemanager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "income_table")
public class Income {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public double amount;
    public String source;
    public String date;
    public String paymentMethod; 

    public Income(double amount, String source, String date, String paymentMethod) {
        this.amount = amount;
        this.source = source;
        this.date = date;
        this.paymentMethod = paymentMethod;
    }
}
