package com.example.pocketpulse;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pp_merchant_memory")
public class PPMerchantMemory {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String originalName;
    public String aliasName;
    public String defaultCategory;
    public long lastUsedDate;

    public PPMerchantMemory(String originalName, String aliasName, String defaultCategory, long lastUsedDate) {
        this.originalName = originalName;
        this.aliasName = aliasName;
        this.defaultCategory = defaultCategory;
        this.lastUsedDate = lastUsedDate;
    }
}