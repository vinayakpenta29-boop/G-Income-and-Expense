package com.example.expensemanager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "source_table")
public class AccountSource {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;

    public AccountSource(String name) {
        this.name = name;
    }
}
