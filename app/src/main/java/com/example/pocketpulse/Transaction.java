package com.example.pocketpulse;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String merchant;
    private double amount;
    private String category;
    private String type;
    private String source;
    private long timestamp;
    private String note; // New field to store your input comments safely

    // 1. ORIGINAL CONSTRUCTOR (Fixes SmsReceiver & your original AddExpenseFragment files)
    public Transaction(double amount, String type, String merchant, String category, String source, long timestamp) {
        this.amount = amount;
        this.type = type;
        this.merchant = merchant;
        this.category = category;
        this.source = source;
        this.timestamp = timestamp;
        this.note = ""; // Default empty note
    }

    // 2. OVERLOADED CONSTRUCTOR (Supports notes directly during instantiation)
    public Transaction(double amount, String type, String merchant, String category, String source, long timestamp, String note) {
        this.amount = amount;
        this.type = type;
        this.merchant = merchant;
        this.category = category;
        this.source = source;
        this.timestamp = timestamp;
        this.note = note;
    }

    // 3. FALLBACK CONSTRUCTOR (Prevents any alternative signature order conflicts)
    public Transaction(String merchant, double amount, String category, String type, String source, long timestamp, String note) {
        this.merchant = merchant;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.source = source;
        this.timestamp = timestamp;
        this.note = note;
    }

    // Empty constructor required by Room implementation layers
    public Transaction() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}