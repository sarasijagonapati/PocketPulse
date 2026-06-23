package com.example.pocketpulse;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<Transaction> getAllTransactions();

    @Insert
    long insertTransaction(Transaction transaction);

    @Query("UPDATE transactions SET category = :category WHERE timestamp = :timestamp")
    void updateCategoryByTimestamp(long timestamp, String category);

    @Update
    void updateTransaction(Transaction transaction);

    @Delete
    void deleteTransaction(Transaction transaction);

    @Query("UPDATE transactions SET category = :category, merchant = :merchant WHERE timestamp = :timestamp")
    void updateCategoryAndMerchantByTimestamp(long timestamp, String category, String merchant);

    // 💳 AUTOMATED BUDGET POOL: Now completely ignores manual/cash entries!
    // It only subtracts debits if they were automatically caught via SMS Auto.
    @Query("SELECT TOTAL(CASE WHEN type = 'Credit' THEN amount WHEN type = 'Debit' AND source = 'SMS Auto' THEN -amount ELSE 0 END) FROM transactions")
    double getRemainingBalance();

    // 💵 CASH SUMMARY POOL: Only calculates manual/hand-entered debits.
    @Query("SELECT TOTAL(amount) FROM transactions WHERE type = 'Debit' AND source != 'SMS Auto'")
    double getTotalManualExpenses();
    // 🛡️ DUPLICATE SHIELD: Prevents logging if caught by notification listener within 45 seconds
    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND type = 'Debit' AND timestamp > :timeWindow")
    int checkDuplicate(double amount, long timeWindow);
}