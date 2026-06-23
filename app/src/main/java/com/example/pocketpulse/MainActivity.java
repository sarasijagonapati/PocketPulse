package com.example.pocketpulse;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.pocketpulse.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Fire up the permission checking pipeline & service checking sequence
        checkAndRequestAppPermissions();

        // 🟢 1.5 ARMED: Kick off the 10:30 PM Daily Summary scheduler thread sequence
        scheduleDailySummaryAlarm(this);

        // 2. Configure bottom navigation bar listeners
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_transactions) {
                selectedFragment = new TransactionFragment();
            } else if (itemId == R.id.nav_add) {
                selectedFragment = new AddExpenseFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    private void checkAndRequestAppPermissions() {
        // 1. Handle regular runtime permissions (SMS & Notifications for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.POST_NOTIFICATIONS
                }, 101);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                }, 101);
            }
        }

        // 2. Automated fallback: Send user to settings screen if system listener is offline
        if (!isNotificationServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ⏰ TIMING UTILITY: Registers repeating alarm execution for exactly 10:30 PM nightly
    private void scheduleDailySummaryAlarm(Context context) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailySummaryReceiver.class);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                999,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        // 🎯 Target exactly 12:00 AM Midnight
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        // If midnight has already elapsed today, defer scheduler bounds to tomorrow's midnight
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            // 🎯 FIXED: Replaced setInexactRepeating with an OS battery-optimization bypass engine
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            android.util.Log.d("PP_MAIN", "⏰ Precise Daily Summary Alarm scheduled successfully for 12:00 AM midnight!");
        }
    }
}