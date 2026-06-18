package com.example.expensemanager.data;

public class TransactionItem {
    private String title;
    private String date;
    private double amount;
    private boolean isIncome;
    private String note;
    private String linkedSourceName;
    private double linkedSourceAvailableBalance;

    public TransactionItem(String title, String date, double amount, boolean isIncome, 
                           String note, String linkedSourceName, double linkedSourceAvailableBalance) {
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.isIncome = isIncome;
        this.note = note;
        this.linkedSourceName = linkedSourceName;
        this.linkedSourceAvailableBalance = linkedSourceAvailableBalance;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
    public String getNote() { return note; }
    public String getLinkedSourceName() { return linkedSourceName; }
    public double getLinkedSourceAvailableBalance() { return linkedSourceAvailableBalance; }
}
