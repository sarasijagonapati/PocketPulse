package com.example.pocketpulse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private ArrayList<Transaction> transactionList;
    private boolean showActions;
    private OnTransactionActionListener actionListener;

    public interface OnTransactionActionListener {
        void onActionComplete();
    }

    public TransactionAdapter(ArrayList<Transaction> transactionList, boolean showActions, OnTransactionActionListener actionListener) {
        this.transactionList = transactionList;
        this.showActions = showActions;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvMerchant.setText(transaction.getMerchant().toUpperCase());
        holder.tvCategory.setText(transaction.getCategory());

        if (transaction.getType().equalsIgnoreCase("Debit")) {
            holder.tvAmount.setText(String.format(java.util.Locale.US, "-₹%.0f", transaction.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.tvAmount.setText(String.format(java.util.Locale.US, "+₹%.0f", transaction.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#22C55E"));
        }

        // Apply visual badge styles matching your categories
        if (transaction.getCategory().equalsIgnoreCase("Food")) {
            holder.tvIconEmoji.setText("🍟");
            holder.tvCategory.setTextColor(Color.parseColor("#EA580C"));
            holder.tvCategory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEDD5")));
        } else if (transaction.getCategory().equalsIgnoreCase("Travel")) {
            holder.tvIconEmoji.setText("🚙");
            holder.tvCategory.setTextColor(Color.parseColor("#2563EB"));
            holder.tvCategory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
        } else if (transaction.getCategory().equalsIgnoreCase("Shopping")) {
            holder.tvIconEmoji.setText("🛍️");
            holder.tvCategory.setTextColor(Color.parseColor("#DB2777"));
            holder.tvCategory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FCE7F3")));
        } else if (transaction.getCategory().equalsIgnoreCase("Utilities")) {
            holder.tvIconEmoji.setText("⚡");
            holder.tvCategory.setTextColor(Color.parseColor("#9333EA"));
            holder.tvCategory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3E8FF")));
        } else {
            holder.tvIconEmoji.setText("💬");
            holder.tvCategory.setTextColor(Color.parseColor("#475569"));
            holder.tvCategory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        }

        if (transaction.getSource().equalsIgnoreCase("Manual")) {
            holder.tvSource.setText("👤 Manual");
        } else {
            holder.tvSource.setText("📱 SMS");
        }

        // FIXED: Convert millisecond database timestamp into clean readable text
        SimpleDateFormat dateFormat = new SimpleDateFormat("• dd MMM", Locale.US);
        String formattedDate = dateFormat.format(new Date(transaction.getTimestamp()));
        holder.tvDate.setText(formattedDate);

        if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
            holder.tvNote.setText("📝 " + transaction.getNote().trim());
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        if (showActions) {
            holder.llActionsMenu.setVisibility(View.VISIBLE);
        } else {
            holder.llActionsMenu.setVisibility(View.GONE);
        }

        holder.ivDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("🗑️ Delete Transaction?")
                    .setMessage("Are you sure you want to remove this transaction entry from your logs?")
                    .setCancelable(true)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            AppDatabase db = AppDatabase.getInstance(context);
                            db.transactionDao().deleteTransaction(transaction);

                            if (context instanceof android.app.Activity) {
                                ((android.app.Activity) context).runOnUiThread(() -> {
                                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show();
                                    if (actionListener != null) actionListener.onActionComplete();
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        holder.ivEdit.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            final EditText etMerchantInput = new EditText(context);
            etMerchantInput.setText(transaction.getMerchant());
            etMerchantInput.setHint("Merchant Name");
            layout.addView(etMerchantInput);

            final EditText etAmountInput = new EditText(context);
            etAmountInput.setText(String.valueOf((int) transaction.getAmount()));
            etAmountInput.setHint("Amount");
            etAmountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            layout.addView(etAmountInput);

            final EditText etNoteInput = new EditText(context);
            etNoteInput.setText(transaction.getNote());
            etNoteInput.setHint("Note (optional)");
            layout.addView(etNoteInput);

            new AlertDialog.Builder(context)
                    .setTitle("✏️ Edit Details")
                    .setView(layout)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newMerchant = etMerchantInput.getText().toString().trim();
                        String newAmountStr = etAmountInput.getText().toString().trim();
                        String newNoteStr = etNoteInput.getText().toString().trim();

                        if (!newMerchant.isEmpty() && !newAmountStr.isEmpty()) {
                            new Thread(() -> {
                                transaction.setMerchant(newMerchant);
                                transaction.setAmount(Double.parseDouble(newAmountStr));
                                transaction.setNote(newNoteStr);

                                AppDatabase db = AppDatabase.getInstance(context);
                                db.transactionDao().updateTransaction(transaction);

                                if (context instanceof android.app.Activity) {
                                    ((android.app.Activity) context).runOnUiThread(() -> {
                                        Toast.makeText(context, "Changes saved!", Toast.LENGTH_SHORT).show();
                                        if (actionListener != null) actionListener.onActionComplete();
                                    });
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvMerchant, tvCategory, tvSource, tvAmount, tvIconEmoji, tvNote, tvDate;
        View ivEdit, ivDelete, llActionsMenu;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMerchant = itemView.findViewById(R.id.tvTransactionMerchant);
            tvCategory = itemView.findViewById(R.id.tvTransactionCategory);
            tvSource = itemView.findViewById(R.id.tvTransactionSource);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvIconEmoji = itemView.findViewById(R.id.tvTransactionIconEmoji);
            tvNote = itemView.findViewById(R.id.tvTransactionNote);

            // Link text field layout target reference
            tvDate = itemView.findViewById(R.id.tvTransactionDate);

            ivEdit = itemView.findViewById(R.id.ivTransactionEdit);
            ivDelete = itemView.findViewById(R.id.ivTransactionDelete);
            llActionsMenu = itemView.findViewById(R.id.llActionsMenu);
        }
    }
}