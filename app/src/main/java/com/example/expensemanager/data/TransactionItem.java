package com.example.expensemanager.data;

public class TransactionItem {
    private String title;
    private String date;
    private double amount;
    private boolean isIncome;

    public TransactionItem(String title, String date, double amount, boolean isIncome) {
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.isIncome = isIncome;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
}
