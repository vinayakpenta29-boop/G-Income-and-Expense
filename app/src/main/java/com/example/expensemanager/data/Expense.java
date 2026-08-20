package com.example.expensemanager.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "expense_table",
        indices = {@Index(value = {"sourceId"})})
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public double amount;
    public String category;    // e.g. Food, Recharge
    public Long sourceId;      // Linked AccountSource ID
    public String sourceName;  // Cached source name (Cash, Bank)
    public String date;
    public String note;

    public Expense(double amount, String category, Long sourceId, String sourceName, String date, String note) {
        this.amount = amount;
        this.category = category;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.date = date;
        this.note = note;
    }
}
