package com.example.expensemanager.data;

public class TransactionItem {
    private long id;
    private String title;
    private String date;
    private double amount;
    private boolean isIncome;
    private String note;
    private Long sourceId;
    private String sourceName;
    private double sourceAvailableBalance;

    public TransactionItem(long id, String title, String date, double amount, boolean isIncome, 
                           String note, Long sourceId, String sourceName, double sourceAvailableBalance) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.isIncome = isIncome;
        this.note = note;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.sourceAvailableBalance = sourceAvailableBalance;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
    public String getNote() { return note; }
    public Long getSourceId() { return sourceId; }
    public String getSourceName() { return sourceName; }
    public double getSourceAvailableBalance() { return sourceAvailableBalance; }
}
