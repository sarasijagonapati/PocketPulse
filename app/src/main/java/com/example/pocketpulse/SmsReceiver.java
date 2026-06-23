package com.example.pocketpulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "PocketPulseParser";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String messageBody = smsMessage.getMessageBody();
                        parseTransaction(context, messageBody);
                    }
                }
            }
        }
    }

    private void parseTransaction(Context context, String smsBody) {
        String type = "Unknown";

        // 1. Classify Transaction Type
        if (smsBody.toLowerCase().contains("credited") || smsBody.toLowerCase().contains("received")) {
            type = "Credit";
        } else if (smsBody.toLowerCase().contains("debited") || smsBody.toLowerCase().contains("sent")) {
            type = "Debit";
        }

        // 🚨 NEW GUARDRAIL: Drop Debits immediately. Accessibility handles them!
        if (!type.equals("Credit")) {
            Log.d(TAG, "🛡️ Skipping SMS: This is a Debit/Unknown text. Accessibility handles this.");
            return;
        }

        // 2. Extract Amount
        String amount = "0.0";
        Pattern amountPattern = Pattern.compile("(?i)(?:Rs\\.?|INR|₹|credited\\s+by|received)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]+)?)");
        Matcher amountMatcher = amountPattern.matcher(smsBody);
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1).replace(",", "");
        }
        if (amount.equals("0.0")) return;

        double finalAmount = Double.parseDouble(amount);

        // 3. Isolate Sender/Merchant Entity
        String merchant = "Unknown Sender";
        Pattern merchantPattern = Pattern.compile("(?i)(?:to|at|from|trf\\s+to)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:Ref|Refno|Ref\\s*no|UPI|Card|on\\s+date|\\-)|$)");
        Matcher merchantMatcher = merchantPattern.matcher(smsBody);
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1).trim();
        }

        if (merchant.contains(":")) {
            merchant = merchant.split(":")[0].trim();
        }

        // 4. Parse Transaction Date
        long txnTimestamp = System.currentTimeMillis();
        try {
            Pattern datePattern = Pattern.compile("(?i)(?:on\\s+date\\s+|date\\s*)([0-9]{2}[A-Za-z]{3}[0-9]{2})");
            Matcher dateMatcher = datePattern.matcher(smsBody);
            if (dateMatcher.find()) {
                String dateStr = dateMatcher.group(1);
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyy", Locale.US);
                Date parsedDate = sdf.parse(dateStr);
                if (parsedDate != null) {
                    txnTimestamp = parsedDate.getTime();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Historical Date parsing failed, fallback used.", e);
        }

        final String finalMerchant = merchant;
        final long finalTimestamp = txnTimestamp;

        // 5. Silent Credit Auto-Logger Pipeline
        Transaction incomingCredit = new Transaction(finalAmount, "Credit", finalMerchant, "Income", "SMS Auto", finalTimestamp);
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                db.transactionDao().insertTransaction(incomingCredit);

                DailySummaryReceiver.triggerPushNotification(
                        context,
                        "💰 Income Auto-Logged (SMS)",
                        "Added ₹" + finalAmount + " from " + incomingCredit.getMerchant() + " directly to Credits.",
                        300
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed logging background credit row from carrier text thread", e);
            }
        }).start();
    }
}