package com.example.pocketpulse;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailySummaryReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "spend_lite_alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("PocketPulsePrefs", Context.MODE_PRIVATE);
        boolean isSummaryEnabled = prefs.getBoolean("daily_summary_enabled", true);

        if (!isSummaryEnabled) return;

        // Query database on a separate background execution thread
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<Transaction> allTxns = db.transactionDao().getAllTransactions();

                double totalDayExpenses = 0;

                // Target "Yesterday" because the clock just hit 12:00 AM midnight
                Calendar targetCal = Calendar.getInstance();
                targetCal.add(Calendar.DAY_OF_YEAR, -1); // Step back 1 day

                int targetYear = targetCal.get(Calendar.YEAR);
                int targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR);

                Calendar txnCal = Calendar.getInstance();

                for (Transaction t : allTxns) {
                    txnCal.setTimeInMillis(t.getTimestamp());

                    // Match transactions against the day that just finished
                    if (txnCal.get(java.util.Calendar.YEAR) == targetYear &&
                            txnCal.get(java.util.Calendar.DAY_OF_YEAR) == targetDayOfYear &&
                            t.getType().equalsIgnoreCase("Debit")) {
                        totalDayExpenses += t.getAmount();
                    }
                }

                String summaryContent = totalDayExpenses > 0
                        ? String.format(Locale.US, "You spent a total of ₹%.0f yesterday.", totalDayExpenses)
                        : "Great job! No expenses logged yesterday.";

                triggerPushNotification(context, "📊 Yesterday's Expense Summary", summaryContent, 200);
            } catch (Exception e) {
                android.util.Log.e("PP_SUMMARY", "Failed to compile midnight summary calculations", e);
            }
        }).start();

        // 🔄 RE-CHAIN TRIGGER: Schedules the exact alarm target for tomorrow midnight
        scheduleDailySummaryAlarm(context);
    }

    private void scheduleDailySummaryAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, DailySummaryReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                999,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        // Target exactly 12:00 AM Midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            // Bypass Android battery-optimization batch shifts to ensure a single precise execution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            android.util.Log.d("PP_SUMMARY", "⏰ Next precise midnight summary link locked in successfully!");
        }
    }

    public static void triggerPushNotification(Context context, String title, String contents, int notificationId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "PocketPulse Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(contents)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}