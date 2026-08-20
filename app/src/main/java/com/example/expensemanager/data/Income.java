package com.example.expensemanager.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "income_table",
        indices = {@Index(value = {"sourceId"})})
public class Income {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public double amount;
    public String title;       // e.g. Salary, Freelance
    public Long sourceId;      // Linked AccountSource ID
    public String sourceName;  // Cached source name (Cash, Bank)
    public String date;
    public String paymentMethod;
    public String note;

    public Income(double amount, String title, Long sourceId, String sourceName, String date, String paymentMethod, String note) {
        this.amount = amount;
        this.title = title;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.note = note;
    }
}
