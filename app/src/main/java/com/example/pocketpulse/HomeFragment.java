package com.example.pocketpulse;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvBudgetAmount, tvSpentAmount, tvLeftAmount, tvUsedPercent, tvPrediction, tvDailyLimit, tvDaysLeftSubtitle, tvTopSpend, tvViewAll;
    private TextView tvFoodSum, tvTravelSum, tvShoppingSum, tvUtilSum;
    private TextView tvHomeMonthPill;
    private LinearProgressIndicator progressBar;
    private PieChartView pieChartView;
    private RecyclerView rvRecent;
    private MaterialCardView mainBudgetCard;

    private TextView tvHomeTotalCashSpent;
    // 🟢 FIXED: Added tracking handle for yesterday's spend UI text view
    private TextView tvHomeYesterdaySpent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mainBudgetCard = view.findViewById(R.id.mainBudgetCard);
        tvBudgetAmount = view.findViewById(R.id.tvHomeBudgetAmount);
        tvSpentAmount = view.findViewById(R.id.tvHomeSpentAmount);
        tvLeftAmount = view.findViewById(R.id.tvHomeLeftAmount);
        tvUsedPercent = view.findViewById(R.id.tvHomeUsedPercentage);
        tvPrediction = view.findViewById(R.id.tvHomePredictionWarning);

        tvDailyLimit = view.findViewById(R.id.tvHomeDailyLimit);
        tvDaysLeftSubtitle = view.findViewById(R.id.tvHomeDaysLeftSubtitle);
        tvTopSpend = view.findViewById(R.id.tvHomeTopSpend);
        tvViewAll = view.findViewById(R.id.tvHomeViewAll);
        tvHomeMonthPill = view.findViewById(R.id.tvHomeMonthPill);

        progressBar = view.findViewById(R.id.budgetProgressBar);
        pieChartView = view.findViewById(R.id.homePieChart);
        rvRecent = view.findViewById(R.id.rvRecentTransactions);

        tvFoodSum = view.findViewById(R.id.tvFoodSummary);
        tvTravelSum = view.findViewById(R.id.tvTravelSummary);
        tvShoppingSum = view.findViewById(R.id.tvShoppingSummary);
        tvUtilSum = view.findViewById(R.id.tvUtilSummary);

        tvHomeTotalCashSpent = view.findViewById(R.id.tvHomeTotalCashSpent);
        // 🟢 FIXED: Bind the layout ID so the text view isn't a null reference
        tvHomeYesterdaySpent = view.findViewById(R.id.tvHomeYesterdaySpent);

        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));

        tvViewAll.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TransactionFragment())
                        .commit();

                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_transactions);
                }
            }
        });

        return view;
    }

    // 🟢 FIXED: Now executes on every single tab focus transition to refresh calculations live
    @Override
    public void onResume() {
        super.onResume();
        loadLiveCalculations();
        loadYesterdayExpense();
    }

    private void loadLiveCalculations() {
        SharedPreferences prefs = getActivity().getSharedPreferences("PocketPulsePrefs", Context.MODE_PRIVATE);
        int monthlyBudget = prefs.getInt("monthly_budget", 5000);
        tvBudgetAmount.setText("₹" + monthlyBudget);

        Calendar calendar = Calendar.getInstance();
        int totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int daysLeft = Math.max(1, (totalDays - currentDay) + 1);

        int currentYear = calendar.get(Calendar.YEAR);
        int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
        tvHomeMonthPill.setText(monthYearFormat.format(calendar.getTime()));
        tvDaysLeftSubtitle.setText(daysLeft + " days left");

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<Transaction> allTransactions = db.transactionDao().getAllTransactions();

            float food = 0, travel = 0, shopping = 0, utilities = 0, others = 0;
            float automatedSpent = 0;
            float cashSpent = 0;
            float totalCredits = 0;
            float todayAutomatedSpent = 0;

            Calendar transCal = Calendar.getInstance();

            for (Transaction t : allTransactions) {
                if (t.getType().equalsIgnoreCase("Debit")) {
                    boolean isAutomated = t.getSource() != null && t.getSource().equalsIgnoreCase("SMS Auto");

                    if (isAutomated) {
                        automatedSpent += t.getAmount();

                        transCal.setTimeInMillis(t.getTimestamp());
                        if (transCal.get(Calendar.YEAR) == currentYear && transCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear) {
                            todayAutomatedSpent += t.getAmount();
                        }
                    } else {
                        cashSpent += t.getAmount();
                    }

                    if (t.getCategory().equalsIgnoreCase("Food")) food += t.getAmount();
                    else if (t.getCategory().equalsIgnoreCase("Travel")) travel += t.getAmount();
                    else if (t.getCategory().equalsIgnoreCase("Shopping")) shopping += t.getAmount();
                    else if (t.getCategory().equalsIgnoreCase("Utilities")) utilities += t.getAmount();
                    else others += t.getAmount();

                } else if (t.getType().equalsIgnoreCase("Credit")) {
                    totalCredits += t.getAmount();
                }
            }

            float finalAutomatedSpent = automatedSpent;
            float finalCashSpent = cashSpent;
            float totalAllSpent = automatedSpent + finalCashSpent;

            float finalFood = food, finalTravel = travel, finalShopping = shopping, finalUtilities = utilities, finalOthers = others;

            float totalLeft = Math.max(0, (monthlyBudget + totalCredits) - finalAutomatedSpent);
            int progressPercent = (monthlyBudget + totalCredits) > 0 ? (int) ((finalAutomatedSpent / (monthlyBudget + totalCredits)) * 100) : 0;

            double calculatedDailyLimit = totalLeft / daysLeft;
            double baselineAllowanceToday = (totalLeft + todayAutomatedSpent) / daysLeft;

            String tmpPredictionText = "🌱 Healthy spending rate";

            if (finalAutomatedSpent > 0) {
                float averageSpendPerDay = finalAutomatedSpent / currentDay;
                float expectedDaysRemaining = totalLeft / averageSpendPerDay;

                if (expectedDaysRemaining < daysLeft) {
                    Calendar exhaustionCalendar = Calendar.getInstance();
                    exhaustionCalendar.add(Calendar.DAY_OF_MONTH, (int) expectedDaysRemaining);

                    SimpleDateFormat targetDateFormat = new SimpleDateFormat("dd MMM", Locale.US);
                    String estimatedFinishDate = targetDateFormat.format(exhaustionCalendar.getTime());

                    tmpPredictionText = "⚠️ Pocket money may finish by " + estimatedFinishDate + "! Save now.";
                } else if (todayAutomatedSpent > baselineAllowanceToday) {
                    tmpPredictionText = "⚠️ Crossed daily limit!";
                }
            } else if (todayAutomatedSpent > baselineAllowanceToday) {
                tmpPredictionText = "⚠️ Crossed daily limit!";
            }

            final String predictionText = tmpPredictionText;

            int targetedCardColor;
            if (progressPercent <= 25) {
                targetedCardColor = Color.parseColor("#16A34A");
            } else if (progressPercent <= 50) {
                targetedCardColor = Color.parseColor("#CA8A04");
            } else if (progressPercent <= 75) {
                targetedCardColor = Color.parseColor("#EF4444");
            } else {
                targetedCardColor = Color.parseColor("#991B1B");
            }

            String topCategory = "—";
            float maxSpend = 0;
            if (finalFood > maxSpend) { maxSpend = finalFood; topCategory = "Food"; }
            if (finalTravel > maxSpend) { maxSpend = finalTravel; topCategory = "Travel"; }
            if (finalShopping > maxSpend) { maxSpend = finalShopping; topCategory = "Shopping"; }
            if (finalUtilities > maxSpend) { maxSpend = finalUtilities; topCategory = "Utilities"; }
            if (finalOthers > maxSpend) { maxSpend = finalOthers; topCategory = "Others"; }
            final String finalTopCategory = topCategory;

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                tvSpentAmount.setText(String.format(Locale.US, "Spent ₹%.0f", finalAutomatedSpent));
                tvLeftAmount.setText(String.format(Locale.US, "Left ₹%.0f", totalLeft));
                tvUsedPercent.setText(String.format(Locale.US, "%.1f%% used", (float)progressPercent));
                tvPrediction.setText(predictionText);

                if (tvHomeTotalCashSpent != null) {
                    tvHomeTotalCashSpent.setText(String.format(Locale.US, "₹ %.2f", finalCashSpent));
                }

                if (mainBudgetCard != null) {
                    mainBudgetCard.setBackgroundTintList(ColorStateList.valueOf(targetedCardColor));
                }

                progressBar.setProgress(Math.min(progressPercent, 100));
                tvDailyLimit.setText(String.format(Locale.US, "₹%.0f", calculatedDailyLimit));
                tvTopSpend.setText(finalTopCategory);

                pieChartView.setChartData(finalFood, finalTravel, finalShopping, finalUtilities, finalOthers);

                if (totalAllSpent > 0) {
                    tvFoodSum.setText(String.format(Locale.US, "%.0f%% · ₹%.0f", (finalFood / totalAllSpent) * 100, finalFood));
                    tvTravelSum.setText(String.format(Locale.US, "%.0f%% · ₹%.0f", (finalTravel / totalAllSpent) * 100, finalTravel));
                    tvShoppingSum.setText(String.format(Locale.US, "%.0f%% · ₹%.0f", (finalShopping / totalAllSpent) * 100, finalShopping));
                    tvUtilSum.setText(String.format(Locale.US, "%.0f%% · ₹%.0f", (finalUtilities / totalAllSpent) * 100, finalUtilities));
                } else {
                    tvFoodSum.setText("0% · ₹0");
                    tvTravelSum.setText("0% · ₹0");
                    tvShoppingSum.setText("0% · ₹0");
                    tvUtilSum.setText("0% · ₹0");
                }

                if (allTransactions.isEmpty()) {
                    rvRecent.setAdapter(new TransactionAdapter(new ArrayList<>(allTransactions), true, () -> loadLiveCalculations()));
                } else {
                    List<Transaction> recentSlice = allTransactions.subList(0, Math.min(allTransactions.size(), 3));
                    rvRecent.setAdapter(new TransactionAdapter(new ArrayList<>(recentSlice), true, () -> loadLiveCalculations()));
                }
            });
        }).start();
    }

    // 🟢 FIXED: No longer floating standalone, now correctly called and uses the global field
    private void loadYesterdayExpense() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            if (db == null) return;
            List<Transaction> allTxns = db.transactionDao().getAllTransactions();

            double yesterdayTotal = 0;

            Calendar targetCal = Calendar.getInstance();
            targetCal.add(Calendar.DAY_OF_YEAR, -1);
            int targetYear = targetCal.get(Calendar.YEAR);
            int targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR);

            Calendar txnCal = Calendar.getInstance();

            for (Transaction t : allTxns) {
                txnCal.setTimeInMillis(t.getTimestamp());
                if (txnCal.get(Calendar.YEAR) == targetYear &&
                        txnCal.get(Calendar.DAY_OF_YEAR) == targetDayOfYear &&
                        t.getType().equalsIgnoreCase("Debit")) {
                    yesterdayTotal += t.getAmount();
                }
            }

            final double finalTotal = yesterdayTotal;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (tvHomeYesterdaySpent != null) {
                        tvHomeYesterdaySpent.setText(String.format(Locale.US, "₹ %.2f", finalTotal));
                    }
                });
            }
        }).start();
    }
}