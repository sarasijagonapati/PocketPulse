package com.example.pocketpulse;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PPMerchantMemory.class}, version = 1, exportSchema = false)
public abstract class AccessibilityDatabase extends RoomDatabase {
    private static volatile AccessibilityDatabase instance;

    public abstract PPMerchantMemoryDao ppMerchantMemoryDao();

    public static AccessibilityDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AccessibilityDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AccessibilityDatabase.class, "accessibility_pulse_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}