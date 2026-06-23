package com.example.pocketpulse;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import java.util.Calendar;

public class SettingsFragment extends Fragment {

    private EditText etBudgetInput;
    private Button btnSaveBudget;
    private CompoundButton switchSmsDetection;
    private CompoundButton switchTxnAlerts, switchDailySummary;
    private MaterialCardView cardResetData;

    // ✅ ADDED: Privacy & Security click target row reference
    private View cardPrivacySecurity;

    private SharedPreferences prefs;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveSmsToggleState(true);
                    Toast.makeText(getContext(), "✅ SMS auto-detection activated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    updateSwitchWithoutTriggeringListener(false);
                    saveSmsToggleState(false);
                    Toast.makeText(getContext(), "❌ Permission denied. Auto-detection disabled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = getActivity().getSharedPreferences("PocketPulsePrefs", Context.MODE_PRIVATE);

        etBudgetInput = view.findViewById(R.id.etSettingsBudget);
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
        switchSmsDetection = view.findViewById(R.id.switchSmsDetection);
        cardResetData = view.findViewById(R.id.cardResetData);
        switchTxnAlerts = view.findViewById(R.id.switchTxnAlerts);
        switchDailySummary = view.findViewById(R.id.switchDailySummary);

        // ✅ ADDED: Bind Privacy UI click row element (Adjust R.id.cardPrivacySecurity if your XML uses a different ID name)
        cardPrivacySecurity = view.findViewById(R.id.cardPrivacySecurity);

        setupBudgetSettingsEngine();
        setupSmsDetectionToggleEngine();
        setupNotificationSettingsEngine();
        setupDatabaseResetEngine();

        // ✅ ADDED: Initialize privacy redirection engine layer
        setupPrivacySecurityEngine();

        return view;
    }

    private void setupBudgetSettingsEngine() {
        int savedBudget = prefs.getInt("monthly_budget", 5000);
        etBudgetInput.setText(String.valueOf(savedBudget));

        btnSaveBudget.setOnClickListener(v -> {
            String budgetStr = etBudgetInput.getText().toString().trim();
            if (!budgetStr.isEmpty()) {
                int newBudget = Integer.parseInt(budgetStr);
                prefs.edit().putInt("monthly_budget", newBudget).apply();
                Toast.makeText(getContext(), "Budget updated to ₹" + newBudget, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSmsDetectionToggleEngine() {
        boolean isSmsPermissionGranted = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean isSmsSettingEnabled = prefs.getBoolean("sms_detection_enabled", false);

        if (isSmsSettingEnabled && !isSmsPermissionGranted) {
            prefs.edit().putBoolean("sms_detection_enabled", false).apply();
            isSmsSettingEnabled = false;
        }

        updateSwitchWithoutTriggeringListener(isSmsSettingEnabled);

        switchSmsDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                    saveSmsToggleState(true);
                    Toast.makeText(getContext(), "✅ SMS auto-detection activated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS);
                }
            } else {
                saveSmsToggleState(false);
                Toast.makeText(getContext(), "⚠️ PocketPulse cannot automatically detect transactions.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupNotificationSettingsEngine() {
        boolean txnAlertsEnabled = prefs.getBoolean("txn_alerts_enabled", true);
        boolean dailySummaryEnabled = prefs.getBoolean("daily_summary_enabled", false);

        switchTxnAlerts.setChecked(txnAlertsEnabled);
        switchDailySummary.setChecked(dailySummaryEnabled);

        switchTxnAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("txn_alerts_enabled", isChecked).apply();
            String toastMsg = isChecked ? "🔔 Instant transaction alerts active" : "🔕 Transaction alerts muted";
            Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
        });

        switchDailySummary.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("daily_summary_enabled", isChecked).apply();
            if (isChecked) {
                configureDaily1030PMAlarm(getContext());
                Toast.makeText(getContext(), "📊 Recap summary scheduled for 10:30 PM daily", Toast.LENGTH_SHORT).show();
            } else {
                cancelDaily1030PMAlarm(getContext());
                Toast.makeText(getContext(), "📊 Recap summary deactivated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configureDaily1030PMAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailySummaryReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar targetTime = Calendar.getInstance();
        targetTime.set(Calendar.HOUR_OF_DAY, 22);
        targetTime.set(Calendar.MINUTE, 30);
        targetTime.set(Calendar.SECOND, 0);

        if (Calendar.getInstance().after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, targetTime.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    private void cancelDaily1030PMAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailySummaryReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void setupDatabaseResetEngine() {
        cardResetData.setOnClickListener(v -> {
            android.view.ContextThemeWrapper wrapper = new android.view.ContextThemeWrapper(
                    getContext(),
                    com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_Alert
            );

            new AlertDialog.Builder(wrapper)
                    .setTitle("⚠️ Reset All App Data?")
                    .setMessage("This will permanently wipe all transactions from your ledger database. This action cannot be undone.")
                    .setCancelable(true)
                    .setPositiveButton("Reset Everything", (dialog, which) -> {
                        new Thread(() -> {
                            AppDatabase db = AppDatabase.getInstance(getContext());
                            db.clearAllTables();
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "💥 Application database cleared completely!", Toast.LENGTH_LONG).show();
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    // ✅ ADDED: Launches your new 3D flip card overlay component cleanly
    private void setupPrivacySecurityEngine() {
        if (cardPrivacySecurity != null) {
            cardPrivacySecurity.setOnClickListener(v -> {
                Intent privacyIntent = new Intent(getActivity(), PrivacyActivity.class);
                startActivity(privacyIntent);
            });
        }
    }

    private void saveSmsToggleState(boolean enabled) {
        prefs.edit().putBoolean("sms_detection_enabled", enabled).apply();
    }

    private void updateSwitchWithoutTriggeringListener(boolean checked) {
        switchSmsDetection.setOnCheckedChangeListener(null);
        switchSmsDetection.setChecked(checked);
        switchSmsDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                    saveSmsToggleState(true);
                    Toast.makeText(getContext(), "✅ SMS auto-detection activated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS);
                }
            } else {
                saveSmsToggleState(false);
                Toast.makeText(getContext(), "⚠️ PocketPulse cannot automatically detect transactions.", Toast.LENGTH_LONG).show();
            }
        });
    }
}