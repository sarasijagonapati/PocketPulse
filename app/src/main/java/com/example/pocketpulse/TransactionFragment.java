package com.example.pocketpulse;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

    // Keeps reference to currently shown list so swipe can find item by position
    private ArrayList<Transaction> currentFilteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        recyclerView      = view.findViewById(R.id.rvAllTransactions);
        tvTotalDebits     = view.findViewById(R.id.tvTotalDebits);
        tvTotalCredits    = view.findViewById(R.id.tvTotalCredits);
        tvNoTransactions  = view.findViewById(R.id.tvNoTransactions);
        etSearch          = view.findViewById(R.id.etSearch);
        spinnerCategory   = view.findViewById(R.id.spinnerCategory);
        spinnerSource     = view.findViewById(R.id.spinnerSource);
        cardDateFilter    = view.findViewById(R.id.cardDateFilter);
        tvDateFilterLabel = view.findViewById(R.id.tvDateFilterLabel);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupFilterSpinners();
        setupSearchListener();
        setupChronologicalDateFilters();
        setupSwipeToEditCategory(); // ← NEW
        loadDatabaseLedger();

        return view;
    }

    // ─────────────────────────────────────────────────────────────
    // NEW: SWIPE LEFT → EDIT CATEGORY
    // ─────────────────────────────────────────────────────────────

    private void setupSwipeToEditCategory() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            // Draw purple reveal background + label while swiping
            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                View item = viewHolder.itemView;

                // Purple background
                android.graphics.drawable.ColorDrawable bg =
                        new android.graphics.drawable.ColorDrawable(Color.parseColor("#6C63FF"));
                bg.setBounds(item.getRight() + (int) dX, item.getTop(),
                        item.getRight(), item.getBottom());
                bg.draw(c);

                // White label text
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(38f);
                paint.setAntiAlias(true);
                String label = "✏️  Edit Category";
                float textWidth = paint.measureText(label);
                float textX = item.getRight() - textWidth - 60;
                float textY = item.getTop() + (item.getHeight() / 2f) + 13f;
                c.drawText(label, textX, textY, paint);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= currentFilteredList.size()) return;

                Transaction transaction = currentFilteredList.get(position);

                // Snap item back — we're not deleting it
                if (recyclerView.getAdapter() != null) {
                    recyclerView.getAdapter().notifyItemChanged(position);
                }

                showEditCategorySheet(transaction);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    // ─────────────────────────────────────────────────────────────
    // NEW: BOTTOM SHEET WITH CATEGORY CHIPS
    // ─────────────────────────────────────────────────────────────

    private void showEditCategorySheet(Transaction transaction) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_edit_category, null);
        sheet.setContentView(sheetView);

        TextView tvTitle              = sheetView.findViewById(R.id.tvCurrentCategory);
        ChipGroup chipGroup           = sheetView.findViewById(R.id.chipGroupCategories);
        android.widget.Button btnSave = sheetView.findViewById(R.id.btnSaveCategory);

        // Show current category as subtitle
        tvTitle.setText("Currently: " + transaction.getCategory());

        // Pre-select the chip that matches current category
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(transaction.getCategory())) {
                chip.setChecked(true);
                break;
            }
        }

        btnSave.setOnClickListener(v -> {
            // Find which chip is selected
            int checkedId = chipGroup.getCheckedChipId();
            if (checkedId == -1) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            Chip selectedChip = sheetView.findViewById(checkedId);
            String newCategory = selectedChip.getText().toString();

            if (newCategory.equalsIgnoreCase(transaction.getCategory())) {
                sheet.dismiss();
                return;
            }

            new Thread(() -> {
                transaction.setCategory(newCategory);
                AppDatabase.getInstance(requireContext())
                        .transactionDao()
                        .updateTransaction(transaction);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "Category updated to " + newCategory + " ✅",
                                Toast.LENGTH_SHORT).show();
                        sheet.dismiss();
                        loadDatabaseLedger();
                    });
                }
            }).start();
        });

        sheet.show();
    }

    // ─────────────────────────────────────────────────────────────
    // YOUR ORIGINAL CODE BELOW — UNCHANGED
    // ─────────────────────────────────────────────────────────────

    private void setupChronologicalDateFilters() {
        cardDateFilter.setOnClickListener(v -> {
            String[] timelineOptions = {"All Time", "Last 7 Days", "Last 7 Weeks", "Specific Day", "Select Month"};
            android.view.ContextThemeWrapper lightWrapper = new android.view.ContextThemeWrapper(
                    getContext(), com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_Alert);
            AlertDialog.Builder builder = new AlertDialog.Builder(lightWrapper);
            builder.setTitle("📅 Select Timeline Window");
            ArrayAdapter<String> dialogAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, timelineOptions) {
                @NonNull @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setTextColor(Color.parseColor("#0F172A"));
                    tv.setTextSize(15f);
                    tv.setPadding(50, 40, 50, 40);
                    return tv;
                }
            };
            builder.setAdapter(dialogAdapter, (dialog, which) -> {
                String selection = timelineOptions[which];
                if (selection.equals("Specific Day")) {
                    activeDateFilter = "Specific Day";
                    Calendar c = Calendar.getInstance();
                    new DatePickerDialog(getContext(), (pv, year, month, day) -> {
                        selectedYear = year; selectedMonth = month; selectedDay = day;
                        tvDateFilterLabel.setText(String.format(Locale.US, "📅 Date: %02d/%02d/%04d", day, month+1, year));
                        filterDataset();
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
                } else if (selection.equals("Select Month")) {
                    activeDateFilter = "Select Month";
                    String[] months = {"January","February","March","April","May","June","July","August","September","October","November","December"};
                    AlertDialog.Builder mb = new AlertDialog.Builder(lightWrapper);
                    mb.setTitle("🔍 Choose Historical Month");
                    ArrayAdapter<String> ma = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, months) {
                        @NonNull @Override
                        public View getView(int pos, @Nullable View cv, @NonNull ViewGroup p) {
                            TextView tv = (TextView) super.getView(pos, cv, p);
                            tv.setTextColor(Color.parseColor("#0F172A")); tv.setTextSize(15f); tv.setPadding(50,40,50,40);
                            return tv;
                        }
                    };
                    mb.setAdapter(ma, (md, mw) -> {
                        selectedMonth = mw; selectedYear = Calendar.getInstance().get(Calendar.YEAR);
                        tvDateFilterLabel.setText("📅 Archive: " + months[mw]); filterDataset();
                    });
                    AlertDialog md2 = mb.create();
                    if (md2.getWindow() != null) md2.getWindow().setBackgroundDrawableResource(android.R.color.white);
                    md2.show();
                } else {
                    activeDateFilter = selection;
                    tvDateFilterLabel.setText("📅 Date Range: " + activeDateFilter);
                    filterDataset();
                }
            });
            AlertDialog d = builder.create();
            if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.white);
            d.show();
        });
    }

    private void setupFilterSpinners() {
        String[] categories = {"All Categories", "Food", "Travel", "Shopping", "Utilities", "Others"};
        String[] sources    = {"All Sources", "SMS Auto", "Manual"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, categories) {
            @NonNull @Override public View getView(int p, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getView(p, cv, parent); ((TextView)v).setTextColor(Color.parseColor("#0F172A")); return v; }
            @Override public View getDropDownView(int p, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(p, cv, parent); v.setBackgroundColor(Color.WHITE);
                TextView tv = v.findViewById(android.R.id.text1); if (tv!=null) tv.setTextColor(Color.parseColor("#0F172A")); return v; }
        };
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);
        ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, sources) {
            @NonNull @Override public View getView(int p, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getView(p, cv, parent); ((TextView)v).setTextColor(Color.parseColor("#0F172A")); return v; }
            @Override public View getDropDownView(int p, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(p, cv, parent); v.setBackgroundColor(Color.WHITE);
                TextView tv = v.findViewById(android.R.id.text1); if (tv!=null) tv.setTextColor(Color.parseColor("#0F172A")); return v; }
        };
        srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(srcAdapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { activeCategoryFilter = categories[pos]; filterDataset(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        spinnerSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { activeSourceFilter = sources[pos]; filterDataset(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { activeSearchQuery = s.toString().trim().toLowerCase(); filterDataset(); }
            @Override public void afterTextChanged(Editable s) {}
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
        double totalDebits = 0, totalCredits = 0;
        long timeNow = System.currentTimeMillis();
        long sevenDaysAgo  = timeNow - (7L * 24 * 60 * 60 * 1000);
        long sevenWeeksAgo = timeNow - (7L * 7 * 24 * 60 * 60 * 1000);
        Calendar itemCal = Calendar.getInstance();
        for (Transaction t : masterList) {
            boolean ms = activeSearchQuery.isEmpty() || t.getMerchant().toLowerCase().contains(activeSearchQuery);
            boolean mc = activeCategoryFilter.equals("All Categories") || t.getCategory().equalsIgnoreCase(activeCategoryFilter);
            boolean mo = activeSourceFilter.equals("All Sources") || t.getSource().equalsIgnoreCase(activeSourceFilter);
            boolean md = true;
            if      (activeDateFilter.equals("Last 7 Days"))  md = t.getTimestamp() >= sevenDaysAgo;
            else if (activeDateFilter.equals("Last 7 Weeks")) md = t.getTimestamp() >= sevenWeeksAgo;
            else if (activeDateFilter.equals("Specific Day")) {
                itemCal.setTimeInMillis(t.getTimestamp());
                md = itemCal.get(Calendar.YEAR)==selectedYear && itemCal.get(Calendar.MONTH)==selectedMonth && itemCal.get(Calendar.DAY_OF_MONTH)==selectedDay;
            } else if (activeDateFilter.equals("Select Month")) {
                itemCal.setTimeInMillis(t.getTimestamp());
                md = itemCal.get(Calendar.YEAR)==selectedYear && itemCal.get(Calendar.MONTH)==selectedMonth;
            }
            if (ms && mc && mo && md) {
                filteredList.add(t);
                if (t.getType().equalsIgnoreCase("Debit"))       totalDebits  += t.getAmount();
                else if (t.getType().equalsIgnoreCase("Credit")) totalCredits += t.getAmount();
            }
        }

        currentFilteredList = filteredList; // ← swipe handler reads from this

        final double fd = totalDebits, fc = totalCredits;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvTotalDebits.setText(String.format(Locale.US, "₹%.0f", fd));
                tvTotalCredits.setText(String.format(Locale.US, "₹%.0f", fc));
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
