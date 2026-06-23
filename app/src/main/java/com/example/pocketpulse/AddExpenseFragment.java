package com.example.pocketpulse;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AddExpenseFragment extends Fragment {

    private EditText etAmount, etMerchant, etNote;
    private MaterialButton btnSave;
    private View chip50, chip100, chip200, chip500;
    private View itemQuickCanteen, itemQuickMetro, itemQuickStationery;

    private MaterialCardView cardFood, cardTravel, cardShopping, cardUtilities, cardOthers;
    private TextView txtFood, txtTravel, txtShopping, txtUtilities, txtOthers;

    // Segmented Entry Toggles
    private Button btnTypeDebit, btnTypeCredit;
    private TextView tvAddTitle, tvMerchantLabel;
    private LinearLayout llCategorySection, llCommonExpensesWrapper;

    private String selectedCategory = "Food";
    private String selectedType = "Debit"; // Tracks mode state ("Debit" or "Credit")

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        // Bind standard headers
        tvAddTitle = view.findViewById(R.id.tvAddTitle);
        tvMerchantLabel = view.findViewById(R.id.tvMerchantLabel);
        etAmount = view.findViewById(R.id.etAddAmount);
        etMerchant = view.findViewById(R.id.etAddMerchant);
        etNote = view.findViewById(R.id.etAddNote);
        btnSave = view.findViewById(R.id.btnSaveExpense);

        // Bind switcher rows
        btnTypeDebit = view.findViewById(R.id.btnTypeDebit);
        btnTypeCredit = view.findViewById(R.id.btnTypeCredit);
        llCategorySection = view.findViewById(R.id.llCategorySection);
        llCommonExpensesWrapper = view.findViewById(R.id.llCommonExpensesWrapper);

        // Bind non-breaking text size amount chips
        chip50 = view.findViewById(R.id.btnChip50);
        chip100 = view.findViewById(R.id.btnChip100);
        chip200 = view.findViewById(R.id.btnChip200);
        chip500 = view.findViewById(R.id.btnChip500);

        cardFood = view.findViewById(R.id.cardFood);
        cardTravel = view.findViewById(R.id.cardTravel);
        cardShopping = view.findViewById(R.id.cardShopping);
        cardUtilities = view.findViewById(R.id.cardUtilities);
        cardOthers = view.findViewById(R.id.cardOthers);

        txtFood = view.findViewById(R.id.txtFood);
        txtTravel = view.findViewById(R.id.txtTravel);
        txtShopping = view.findViewById(R.id.txtShopping);
        txtUtilities = view.findViewById(R.id.txtUtilities);
        txtOthers = view.findViewById(R.id.txtOthers);

        itemQuickCanteen = view.findViewById(R.id.itemQuickCanteen);
        itemQuickMetro = view.findViewById(R.id.itemQuickMetro);
        itemQuickStationery = view.findViewById(R.id.itemQuickStationery);

        setupQuickAmountTriggers();
        setupCategoryCardSelectionEngine();
        setupCommonShortcutListeners();
        setupSegmentedTypeToggleEngine(); // Start mode selection listeners

        btnSave.setOnClickListener(v -> saveExpenseToDatabase());

        return view;
    }

    private void setupSegmentedTypeToggleEngine() {
        // Option A: Expense Mode Selected
        btnTypeDebit.setOnClickListener(v -> {
            selectedType = "Debit";
            tvAddTitle.setText("Add Expense");
            tvMerchantLabel.setText("Merchant / Place *");
            etMerchant.setHint("e.g., College Canteen");

            llCategorySection.setVisibility(View.VISIBLE);
            llCommonExpensesWrapper.setVisibility(View.VISIBLE);

            // Highlighting tab background to dark slate-black
            btnTypeDebit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0F172A")));
            btnTypeDebit.setTextColor(Color.WHITE);

            btnTypeCredit.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnTypeCredit.setTextColor(Color.parseColor("#475569"));

            btnSave.setText("Add Expense");
            btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0F172A")));
        });

        // Option B: Income Mode Selected
        btnTypeCredit.setOnClickListener(v -> {
            selectedType = "Credit";
            selectedCategory = "Others"; // Defaults incoming money classification cleanly to Others
            tvAddTitle.setText("Add Income / Allowance");
            tvMerchantLabel.setText("Source / Received From *");
            etMerchant.setHint("e.g., Pocket Money, Gift, Salary");

            // Hide unneeded expense shortcuts
            llCategorySection.setVisibility(View.GONE);
            llCommonExpensesWrapper.setVisibility(View.GONE);

            // Highlighting tab background to vibrant green profile theme
            btnTypeCredit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#22C55E")));
            btnTypeCredit.setTextColor(Color.WHITE);

            btnTypeDebit.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnTypeDebit.setTextColor(Color.parseColor("#475569"));

            btnSave.setText("Add Income Funds");
            btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#22C55E")));
        });
    }

    private void setupQuickAmountTriggers() {
        chip50.setOnClickListener(v -> etAmount.setText("50"));
        chip100.setOnClickListener(v -> etAmount.setText("100"));
        chip200.setOnClickListener(v -> etAmount.setText("200"));
        chip500.setOnClickListener(v -> etAmount.setText("500"));
    }

    private void setupCategoryCardSelectionEngine() {
        cardFood.setOnClickListener(v -> selectCategoryState("Food"));
        cardTravel.setOnClickListener(v -> selectCategoryState("Travel"));
        cardShopping.setOnClickListener(v -> selectCategoryState("Shopping"));
        cardUtilities.setOnClickListener(v -> selectCategoryState("Utilities"));
        cardOthers.setOnClickListener(v -> selectCategoryState("Others"));
    }

    private void selectCategoryState(String categoryName) {
        selectedCategory = categoryName;

        resetCardState(cardFood, txtFood, "#EA580C", "#FFFFFF", false);
        resetCardState(cardTravel, txtTravel, "#2563EB", "#FFFFFF", false);
        resetCardState(cardShopping, txtShopping, "#DB2777", "#FFFFFF", false);
        resetCardState(cardUtilities, txtUtilities, "#9333EA", "#FFFFFF", false);
        resetCardState(cardOthers, txtOthers, "#475569", "#FFFFFF", false);

        if (categoryName.equalsIgnoreCase("Food")) {
            highlightCardState(cardFood, txtFood, "#EA580C", "#FFEDD5");
        } else if (categoryName.equalsIgnoreCase("Travel")) {
            highlightCardState(cardTravel, txtTravel, "#2563EB", "#EFF6FF");
        } else if (categoryName.equalsIgnoreCase("Shopping")) {
            highlightCardState(cardShopping, txtShopping, "#DB2777", "#FCE7F3");
        } else if (categoryName.equalsIgnoreCase("Utilities")) {
            highlightCardState(cardUtilities, txtUtilities, "#9333EA", "#F3E8FF");
        } else if (categoryName.equalsIgnoreCase("Others")) {
            highlightCardState(cardOthers, txtOthers, "#475569", "#F1F5F9");
        }
    }

    private void highlightCardState(MaterialCardView card, TextView text, String colorHex, String bgHex) {
        card.setStrokeColor(Color.parseColor(colorHex));
        card.setStrokeWidth(4);
        card.setBackgroundColor(Color.parseColor(bgHex));
        text.setTextColor(Color.parseColor(colorHex));
        text.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetCardState(MaterialCardView card, TextView text, String normalColorHex, String bgHex, boolean isBold) {
        card.setStrokeColor(Color.parseColor("#CBD5E1"));
        card.setStrokeWidth(2);
        card.setBackgroundColor(Color.parseColor(bgHex));
        text.setTextColor(Color.parseColor("#475569"));
        text.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void setupCommonShortcutListeners() {
        itemQuickCanteen.setOnClickListener(v -> {
            etMerchant.setText("College Canteen");
            etAmount.setText("150");
            selectCategoryState("Food");
        });

        itemQuickMetro.setOnClickListener(v -> {
            etMerchant.setText("Metro");
            etAmount.setText("60");
            selectCategoryState("Travel");
        });

        itemQuickStationery.setOnClickListener(v -> {
            etMerchant.setText("Stationery Shop");
            etAmount.setText("200");
            selectCategoryState("Shopping");
        });
    }

    private void saveExpenseToDatabase() {
        String merchantStr = etMerchant.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();

        if (merchantStr.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(getContext(), "Please complete all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountVal = Double.parseDouble(amountStr);
        long currentTimestamp = System.currentTimeMillis();

        Transaction targetTransaction = new Transaction(
                amountVal,
                selectedType, // Saves active selection type value dynamically ("Debit" or "Credit")
                merchantStr,
                selectedCategory,
                "Manual",
                currentTimestamp,
                noteStr
        );

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            db.transactionDao().insertTransaction(targetTransaction);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String alertMsg = selectedType.equals("Credit") ? "Income added successfully!" : "Expense logged successfully!";
                    Toast.makeText(getContext(), alertMsg, Toast.LENGTH_SHORT).show();

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();

                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                });
            }
        }).start();
    }
}