package com.example.expensemanager.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensemanager.R;
import com.example.expensemanager.data.TransactionItem;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionItem> transactionList = new ArrayList<>();

    public void updateData(List<TransactionItem> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionItem item = transactionList.get(position);
        holder.tvRowTitle.setText(item.getTitle());
        holder.tvRowDate.setText(item.getDate());

        if (item.isIncome()) {
            holder.tvRowAmount.setText("+ ₹" + item.getAmount());
            holder.tvRowAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
        } else {
            holder.tvRowAmount.setText("- ₹" + item.getAmount());
            holder.tvRowAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
        }

        // ATTACH CLICK ACTION TRIGGER TO RUN OVERLAY DETAILS VIEW DIALOG
        holder.itemView.setOnClickListener(v -> showPremiumDetailsPopUp(v.getContext(), item));
    }

    private void showPremiumDetailsPopUp(android.content.Context context, TransactionItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_transaction_details, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Bind overlay controls
        TextView tvDlgType = dialogView.findViewById(R.id.tvDlgType);
        TextView tvDlgTitle = dialogView.findViewById(R.id.tvDlgTitle);
        TextView tvDlgDate = dialogView.findViewById(R.id.tvDlgDate);
        TextView tvDlgAmount = dialogView.findViewById(R.id.tvDlgAmount);
        TextView tvDlgNote = dialogView.findViewById(R.id.tvDlgNote);
        LinearLayout layoutDlgExpenseDetails = dialogView.findViewById(R.id.layoutDlgExpenseDetails);
        TextView tvDlgLinkedSource = dialogView.findViewById(R.id.tvDlgLinkedSource);
        TextView tvDlgSourceBalance = dialogView.findViewById(R.id.tvDlgSourceBalance);
        Button btnDlgClose = dialogView.findViewById(R.id.btnDlgClose);

        // Map general structural information variables
        tvDlgTitle.setText(item.getTitle());
        tvDlgDate.setText("Date Logged: " + item.getDate());
        tvDlgNote.setText(item.getNote() != null && !item.getNote().isEmpty() ? item.getNote() : "No contextual notes logged.");

        if (item.isIncome()) {
            tvDlgType.setText("INCOME TRANSACTION RECORD");
            tvDlgAmount.setText("+ ₹" + item.getAmount());
            tvDlgAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));
            layoutDlgExpenseDetails.setVisibility(View.GONE);
        } else {
            tvDlgType.setText("EXPENSE TRANSACTION RECORD");
            tvDlgAmount.setText("- ₹" + item.getAmount());
            tvDlgAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
            
            // Render funding source profile contextual calculations
            layoutDlgExpenseDetails.setVisibility(View.VISIBLE);
            tvDlgLinkedSource.setText(item.getLinkedSourceName());
            tvDlgSourceBalance.setText("₹" + item.getLinkedSourceAvailableBalance());
        }

        btnDlgClose.setOnClickListener(v -> dialog.dismiss());

        // CRITICAL FOR CARD CURVES: Masks background system canvas framing blocks clear transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }

    @Override
    public int getItemCount() { return transactionList.size(); }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvRowTitle, tvRowDate, tvRowAmount;
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowTitle = itemView.findViewById(R.id.tvRowTitle);
            tvRowDate = itemView.findViewById(R.id.tvRowDate);
            tvRowAmount = itemView.findViewById(R.id.tvRowAmount);
        }
    }
}
