package com.example.pocketpulse;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PPMerchantMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveMerchantMemory(PPMerchantMemory merchantMemory);

    @Query("SELECT * FROM pp_merchant_memory WHERE originalName = :originalName LIMIT 1")
    PPMerchantMemory getMerchantByOriginalName(String originalName);
}