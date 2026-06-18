package com.example.expensemanager.data;

public class TransactionItem {
    private long id; // Track internal Room database primary key
    private String title;
    private String date;
    private double amount;
    private boolean isIncome;
    private String note;
    private String linkedSourceName;
    private Long linkedSourceId; // Track linked source ID context
    private double linkedSourceAvailableBalance;

    public TransactionItem(long id, String title, String date, double amount, boolean isIncome, 
                           String note, String linkedSourceName, Long linkedSourceId, double linkedSourceAvailableBalance) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.isIncome = isIncome;
        this.note = note;
        this.linkedSourceName = linkedSourceName;
        this.linkedSourceId = linkedSourceId;
        this.linkedSourceAvailableBalance = linkedSourceAvailableBalance;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
    public String getNote() { return note; }
    public String getLinkedSourceName() { return linkedSourceName; }
    public Long getLinkedSourceId() { return linkedSourceId; }
    public double getLinkedSourceAvailableBalance() { return linkedSourceAvailableBalance; }
}
