package com.example.pocketpulse;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PocketPulseRepository {
    private final AccessibilityDatabase accDatabase;
    private final AppDatabase mainDatabase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback<T> {
        void onComplete(T result);
    }

    public PocketPulseRepository(Context context) {
        this.accDatabase = AccessibilityDatabase.getInstance(context);
        this.mainDatabase = AppDatabase.getInstance(context);
    }

    public void checkDuplicateTransaction(double amount, Callback<Boolean> callback) {
        executor.execute(() -> {
            long safeWindow = System.currentTimeMillis() - 45000;
            // Now communicates perfectly with your updated TransactionDao
            int count = mainDatabase.transactionDao().checkDuplicate(amount, safeWindow);
            callback.onComplete(count > 0);
        });
    }

    public void getMerchantDetails(String originalName, Callback<PPMerchantMemory> callback) {
        executor.execute(() -> {
            PPMerchantMemory memory = accDatabase.ppMerchantMemoryDao().getMerchantByOriginalName(originalName);
            callback.onComplete(memory);
        });
    }

    public void bridgeToMainLedger(double amount, String rawRecipient, String alias, String category) {
        executor.execute(() -> {
            PPMerchantMemory memory = new PPMerchantMemory(rawRecipient, alias, category, System.currentTimeMillis());
            accDatabase.ppMerchantMemoryDao().saveMerchantMemory(memory);

            String finalizedDisplayName = alias.isEmpty() ? rawRecipient : alias;

            // Creates the transaction using your exact parameter order
            Transaction mainTx = new Transaction(
                    amount,
                    "Debit",
                    finalizedDisplayName,
                    category,
                    "SMS Auto", // Automatically updates your remaining balance pool equation!
                    System.currentTimeMillis()
            );

            // Calls your matching insert method
            mainDatabase.transactionDao().insertTransaction(mainTx);
        });
    }
}