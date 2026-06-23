package com.example.pocketpulse;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvTotalDebits, tvTotalCredits, tvNoTransactions, tvDateFilterLabel;
    private EditText etSearch;
    private Spinner spinnerCategory, spinnerSource;
    private View cardDateFilter;

    private List<Transaction> masterList = new ArrayList<>();
    private String activeSearchQuery = "";
    private String activeCategoryFilter = "All Categories";
    private String activeSourceFilter = "All Sources";

    private String activeDateFilter = "All Time";
    private int selectedYear, selectedMonth, selectedDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        recyclerView = view.findViewById(R.id.rvAllTransactions);
        tvTotalDebits = view.findViewById(R.id.tvTotalDebits);
        tvTotalCredits = view.findViewById(R.id.tvTotalCredits);
        tvNoTransactions = view.findViewById(R.id.tvNoTransactions);
        etSearch = view.findViewById(R.id.etSearch);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerSource = view.findViewById(R.id.spinnerSource);

        cardDateFilter = view.findViewById(R.id.cardDateFilter);
        tvDateFilterLabel = view.findViewById(R.id.tvDateFilterLabel);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupFilterSpinners();
        setupSearchListener();
        setupChronologicalDateFilters();
        loadDatabaseLedger();

        return view;
    }

    private void setupChronologicalDateFilters() {
        cardDateFilter.setOnClickListener(v -> {
            String[] timelineOptions = {"All Time", "Last 7 Days", "Last 7 Weeks", "Specific Day", "Select Month"};

            // 1. FORCED LIGHT STYLE WRAPPER (Overrides forced system dark modes cleanly)
            android.view.ContextThemeWrapper lightWrapper = new android.view.ContextThemeWrapper(
                    getContext(),
                    com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_Alert
            );

            AlertDialog.Builder builder = new AlertDialog.Builder(lightWrapper);
            builder.setTitle("📅 Select Timeline Window");

            // 2. CUSTOM TEXT ADAPTER (Forces text items to render in crisp slate-black text)
            ArrayAdapter<String> dialogAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, timelineOptions) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView textView = (TextView) super.getView(position, convertView, parent);
                    textView.setTextColor(Color.parseColor("#0F172A")); // Locks text color to black/slate
                    textView.setTextSize(15f);
                    textView.setPadding(50, 40, 50, 40);
                    return textView;
                }
            };

            builder.setAdapter(dialogAdapter, (dialog, which) -> {
                String selection = timelineOptions[which];

                if (selection.equals("Specific Day")) {
                    activeDateFilter = "Specific Day";
                    Calendar c = Calendar.getInstance();
                    DatePickerDialog dpd = new DatePickerDialog(getContext(), (pickerView, year, month, dayOfMonth) -> {
                        selectedYear = year;
                        selectedMonth = month;
                        selectedDay = dayOfMonth;

                        tvDateFilterLabel.setText(String.format(Locale.US, "📅 Date: %02d/%02d/%04d", dayOfMonth, month + 1, year));
                        filterDataset();
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    dpd.show();

                } else if (selection.equals("Select Month")) {
                    activeDateFilter = "Select Month";
                    String[] monthsList = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(lightWrapper);
                    mBuilder.setTitle("🔍 Choose Historical Month");

                    ArrayAdapter<String> monthsAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, monthsList) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            TextView tv = (TextView) super.getView(position, convertView, parent);
                            tv.setTextColor(Color.parseColor("#0F172A")); // Locks text color to black/slate
                            tv.setTextSize(15f);
                            tv.setPadding(50, 40, 50, 40);
                            return tv;
                        }
                    };

                    mBuilder.setAdapter(monthsAdapter, (mDialog, mWhich) -> {
                        selectedMonth = mWhich;
                        selectedYear = Calendar.getInstance().get(Calendar.YEAR);
                        tvDateFilterLabel.setText("📅 Archive: " + monthsList[mWhich]);
                        filterDataset();
                    });

                    AlertDialog mDialog = mBuilder.create();
                    // 3. FORCE WHITE CANVAS BACKGROUND (Prevents dark layout bleed through)
                    if (mDialog.getWindow() != null) {
                        mDialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
                    }
                    mDialog.show();

                } else {
                    activeDateFilter = selection;
                    tvDateFilterLabel.setText("📅 Date Range: " + activeDateFilter);
                    filterDataset();
                }
            });

            AlertDialog dialog = builder.create();
            // 4. FORCE WHITE CANVAS BACKGROUND
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            }
            dialog.show();
        });
    }

    private void setupFilterSpinners() {
        String[] categories = {"All Categories", "Food", "Travel", "Shopping", "Utilities", "Others"};
        String[] sources = {"All Sources", "SMS Auto", "Manual"};

        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, categories) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.parseColor("#0F172A"));
                return v;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(Color.WHITE);
                TextView tv = v.findViewById(android.R.id.text1);
                if (tv != null) tv.setTextColor(Color.parseColor("#0F172A"));
                return v;
            }
        };
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, sources) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.parseColor("#0F172A"));
                return v;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(Color.WHITE);
                TextView tv = v.findViewById(android.R.id.text1);
                if (tv != null) tv.setTextColor(Color.parseColor("#0F172A"));
                return v;
            }
        };
        srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(srcAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeCategoryFilter = categories[position];
                filterDataset();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeSourceFilter = sources[position];
                filterDataset();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearchQuery = s.toString().trim().toLowerCase();
                filterDataset();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDatabaseLedger() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            masterList = db.transactionDao().getAllTransactions();
            filterDataset();
        }).start();
    }

    private void filterDataset() {
        ArrayList<Transaction> filteredList = new ArrayList<>();
        double totalDebits = 0;
        double totalCredits = 0;

        long timeNow = System.currentTimeMillis();
        long sevenDaysAgo = timeNow - (7L * 24 * 60 * 60 * 1000);
        long sevenWeeksAgo = timeNow - (7L * 7 * 24 * 60 * 60 * 1000);

        Calendar itemCal = Calendar.getInstance();

        for (Transaction t : masterList) {
            boolean matchesSearch = activeSearchQuery.isEmpty() || t.getMerchant().toLowerCase().contains(activeSearchQuery);
            boolean matchesCategory = activeCategoryFilter.equals("All Categories") || t.getCategory().equalsIgnoreCase(activeCategoryFilter);
            boolean matchesSource = activeSourceFilter.equals("All Sources") || t.getSource().equalsIgnoreCase(activeSourceFilter);

            boolean matchesDateRange = true;
            if (activeDateFilter.equals("Last 7 Days")) {
                matchesDateRange = t.getTimestamp() >= sevenDaysAgo;
            } else if (activeDateFilter.equals("Last 7 Weeks")) {
                matchesDateRange = t.getTimestamp() >= sevenWeeksAgo;
            } else if (activeDateFilter.equals("Specific Day")) {
                itemCal.setTimeInMillis(t.getTimestamp());
                matchesDateRange = (itemCal.get(Calendar.YEAR) == selectedYear &&
                        itemCal.get(Calendar.MONTH) == selectedMonth &&
                        itemCal.get(Calendar.DAY_OF_MONTH) == selectedDay);
            } else if (activeDateFilter.equals("Select Month")) {
                itemCal.setTimeInMillis(t.getTimestamp());
                matchesDateRange = (itemCal.get(Calendar.YEAR) == selectedYear &&
                        itemCal.get(Calendar.MONTH) == selectedMonth);
            }

            if (matchesSearch && matchesCategory && matchesSource && matchesDateRange) {
                filteredList.add(t);
                if (t.getType().equalsIgnoreCase("Debit")) {
                    totalDebits += t.getAmount();
                } else if (t.getType().equalsIgnoreCase("Credit")) {
                    totalCredits += t.getAmount();
                }
            }
        }

        final double finalDebits = totalDebits;
        final double finalCredits = totalCredits;

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvTotalDebits.setText(String.format(Locale.US, "₹%.0f", finalDebits));
                tvTotalCredits.setText(String.format(Locale.US, "₹%.0f", finalCredits));

                if (filteredList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvNoTransactions.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoTransactions.setVisibility(View.GONE);
                }

                TransactionAdapter adapter = new TransactionAdapter(filteredList, true, () -> loadDatabaseLedger());
                recyclerView.setAdapter(adapter);
            });
        }
    }
}