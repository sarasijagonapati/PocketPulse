package com.example.pocketpulse;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.RemoteInput;

public class CategoryActionReceiver extends BroadcastReceiver {

    private static final String TAG = "PocketPulseActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        long timestamp = intent.getLongExtra("txn_timestamp", -1);
        String chosenCategory = intent.getStringExtra("chosen_category");
        String originalMerchant = intent.getStringExtra("merchant_name");
        int notificationId = intent.getIntExtra("notification_id", 100);

        if (timestamp != -1 && chosenCategory != null && originalMerchant != null) {

            // 🛡️ MULTI-CLICK LOCK: Check if this unique notification timestamp has already been answered
            String lockKey = "lock_" + timestamp;
            SharedPreferences lockPrefs = context.getSharedPreferences("PocketPulseClickLock", Context.MODE_PRIVATE);
            if (lockPrefs.getBoolean(lockKey, false)) {
                Log.d(TAG, "Notification already processed. Blocking subsequent button tap modification.");
                return;
            }

            // Extract text string input note from direct reply fields
            Bundle remoteInputResult = RemoteInput.getResultsFromIntent(intent);
            String customNoteText = "";
            if (remoteInputResult != null) {
                CharSequence charSequence = remoteInputResult.getCharSequence(UPINotificationListener.KEY_TEXT_REPLY);
                if (charSequence != null) {
                    customNoteText = charSequence.toString().trim();
                }
            }

            // If user wrote a note, append it gracefully -> "Amma (testing)" instead of replacing it
            final String finalMerchantName;
            if (!customNoteText.isEmpty()) {
                finalMerchantName = originalMerchant + " (" + customNoteText + ")";
            } else {
                finalMerchantName = originalMerchant;
            }

            // =========================================================================
            // ✅ FIXED: Create an unchangeable final boolean flag for lambda capture
            // =========================================================================
            final boolean hasNote = !customNoteText.isEmpty();
            // =========================================================================

            // Lock this transaction ID immediately on the main thread so future taps are rejected
            lockPrefs.edit().putBoolean(lockKey, true).apply();

            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(context);
                    db.transactionDao().updateCategoryAndMerchantByTimestamp(timestamp, chosenCategory, finalMerchantName);

                    // FIXED: Using the final hasNote variable to satisfy compiler scope bounds
                    if (hasNote) {
                        showToast(context, "📝 Saved to Others: " + finalMerchantName);
                    } else {
                        showToast(context, "💥 Categorized as " + chosenCategory);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Database transaction category swap runtime breakdown", e);
                    lockPrefs.edit().remove(lockKey).apply();
                }

                // Clean notification banner card out of your status bar tray view layout
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(notificationId);
                }
            }).start();
        }
    }

    private void showToast(Context context, String alertMessage) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, alertMessage, Toast.LENGTH_SHORT).show();
        });
    }
}