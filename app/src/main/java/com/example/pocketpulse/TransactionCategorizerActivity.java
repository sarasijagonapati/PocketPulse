package com.example.pocketpulse;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class TransactionCategorizerActivity extends AppCompatActivity {
    private PocketPulseRepository repository;
    private double amount;
    private String rawRecipient;
    private String selectedCategory = "Other";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new PocketPulseRepository(this);

        amount = getIntent().getDoubleExtra("EXTRA_AMOUNT", 0.0);
        rawRecipient = getIntent().getStringExtra("EXTRA_RECIPIENT");

        displayBottomSheet();
    }

    private void displayBottomSheet() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_categorizer, null);
        sheetDialog.setContentView(view);

        EditText etAlias = view.findViewById(R.id.etRecipientName);
        TextView tvAmount = view.findViewById(R.id.tvAmountText);
        TextView tvSuggest = view.findViewById(R.id.tvSuggestion);
        ChipGroup chipGroup = view.findViewById(R.id.categoryChipGroup);
        EditText etCustomCategory = view.findViewById(R.id.etCustomCategory);
        Button btnSave = view.findViewById(R.id.btnSaveTransaction);

        tvAmount.setText("₹" + amount);
        etAlias.setText(rawRecipient);

        repository.getMerchantDetails(rawRecipient, memory -> {
            if (memory != null) {
                runOnUiThread(() -> {
                    if (memory.aliasName != null && !memory.aliasName.isEmpty()) {
                        etAlias.setText(memory.aliasName);
                    }
                    if (memory.defaultCategory != null) {
                        tvSuggest.setVisibility(View.VISIBLE);
                        tvSuggest.setText("Suggested Category: " + memory.defaultCategory);
                        selectedCategory = memory.defaultCategory;
                        autoSelectChip(chipGroup, memory.defaultCategory);
                    }
                });
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                selectedCategory = chip.getText().toString();

                if (selectedCategory.equalsIgnoreCase("Other")) {
                    etCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    etCustomCategory.setVisibility(View.GONE);
                    etCustomCategory.setText("");
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            String finalAlias = etAlias.getText().toString().trim();
            String finalCategory = selectedCategory;

            if (selectedCategory.equalsIgnoreCase("Other")) {
                String customValue = etCustomCategory.getText().toString().trim();
                if (!customValue.isEmpty()) {
                    finalCategory = customValue;
                }
            }

            repository.bridgeToMainLedger(amount, rawRecipient, finalAlias, finalCategory);
            Toast.makeText(this, "Logged to PocketPulse Ledger!", Toast.LENGTH_SHORT).show();
            sheetDialog.dismiss();
            finish();
        });

        sheetDialog.setOnCancelListener(dialog -> finish());
        sheetDialog.show();
    }

    private void autoSelectChip(ChipGroup group, String category) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(category)) {
                chip.setChecked(true);
                break;
            }
        }
    }
}