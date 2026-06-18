package com.example.expensemanager.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// FIX: Added indices block to speed up cross-table queries and drop the warning log
@Entity(tableName = "expense_table",
        foreignKeys = @ForeignKey(entity = Income.class,
                parentColumns = "id",
                childColumns = "incomeSourceId",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index(value = {"incomeSourceId"})})
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public double amount;
    public String category;
    public Long incomeSourceId; 
    public String date;
    public String note;

    public Expense(double amount, String category, Long incomeSourceId, String date, String note) {
        this.amount = amount;
        this.category = category;
        this.incomeSourceId = incomeSourceId;
        this.date = date;
        this.note = note;
    }
}
