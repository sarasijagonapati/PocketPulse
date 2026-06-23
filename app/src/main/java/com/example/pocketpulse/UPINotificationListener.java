package com.example.pocketpulse;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UPINotificationListener extends NotificationListenerService {

    private static final String TAG = "PocketPulseNotifCatch";
    private static final String CHANNEL_ID = "spend_lite_alerts";
    public static final String KEY_TEXT_REPLY = "key_text_reply";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        boolean isPaymentApp = packageName.equals("com.phonepe.app") ||
                packageName.equals("com.google.android.apps.nbu.paisa.user") ||
                packageName.equals("net.one97.paytm");

        if (!isPaymentApp) return;

        android.app.Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        if (extras != null) {
            String title = extras.getString(android.app.Notification.EXTRA_TITLE, "UPI Transaction");

            StringBuilder bodyBuilder = new StringBuilder();
            if (extras.getCharSequence(android.app.Notification.EXTRA_TEXT) != null) {
                bodyBuilder.append(extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString()).append("\n");
            }
            CharSequence[] textLines = extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES);
            if (textLines != null) {
                for (CharSequence line : textLines) {
                    if (line != null) bodyBuilder.append(line.toString()).append("\n");
                }
            }

            String fullContent = bodyBuilder.toString();
            String[] lines = fullContent.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    processTransactionFlow(title, line.trim());
                }
            }
        }
    }

    private void processTransactionFlow(String title, String lineContent) {
        String lowerLine = lineContent.toLowerCase();

        // 🚨 1. ARCHITECTURE BOUNDARY: Instantly ignore and drop Debits. Accessibility handles them on-screen!
        boolean isInboundCredit = lowerLine.contains("received") || lowerLine.contains("credited") || lowerLine.contains("to you") || lowerLine.contains("sent you");
        if (!isInboundCredit) {
            Log.d(TAG, "🛡️ Skipping Push Notification: This is a Debit. Accessibility Service handles this.");
            return;
        }

        String amount = "0.0";

        // 2. Extract Amount
        Pattern amountPattern = Pattern.compile("(?i)(?:Rs\\.?|INR|₹|debited|sent|received|paid)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]+)?)");
        Matcher amountMatcher = amountPattern.matcher(lineContent);
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1).replace(",", "");
        }
        if (amount.equals("0.0")) return;
        double finalAmount = Double.parseDouble(amount);
        long txnTimestamp = System.currentTimeMillis();

        // 3. Identify Sender Name
        String merchantName = title;
        Pattern namePattern = Pattern.compile("(?i)(?:to|from)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:via|successfully|to you)|$)");
        Matcher nameMatcher = namePattern.matcher(lineContent);
        if (nameMatcher.find()) {
            String extractedName = nameMatcher.group(1).trim();
            if (!extractedName.equalsIgnoreCase("you")) {
                merchantName = extractedName;
            }
        }

        // Clean up duplicate system colons ("Amma : Amma")
        if (merchantName.contains(":")) {
            merchantName = merchantName.split(":")[0].trim();
        }

        final String finalMerchant = merchantName;
        String typeStr = "Credit";

        // 🛡️ ANTI-DUPLICATE GATE: Check if this exact line was parsed recently
        String uniqueLineSignature = typeStr + "-" + finalMerchant + "-" + amount + "-" + lineContent.hashCode();
        SharedPreferences cachePrefs = getSharedPreferences("PocketPulseCache", Context.MODE_PRIVATE);
        long lastParsedTime = cachePrefs.getLong(uniqueLineSignature, 0);

        if (System.currentTimeMillis() - lastParsedTime < 600000) {
            Log.d(TAG, "Duplicate prevention system triggered. Skipping line: " + lineContent);
            return;
        }

        cachePrefs.edit().putLong(uniqueLineSignature, System.currentTimeMillis()).apply();

        // 🟢 SCENARIO B: Income tracking auto-save pipeline (Completely restriction-free)
        Transaction incomingCredit = new Transaction(finalAmount, "Credit", finalMerchant, "Income", "SMS Auto", txnTimestamp);
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.transactionDao().insertTransaction(incomingCredit);

                DailySummaryReceiver.triggerPushNotification(
                        getApplicationContext(),
                        "💰 Income Auto-Logged",
                        "Added ₹" + finalAmount + " from " + incomingCredit.getMerchant() + " directly to Credits.",
                        300
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed logging background credit row", e);
            }
        }).start();
    }
}