package com.example.expensemanager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensemanager.R;
import com.example.expensemanager.data.AppDatabase;
import com.example.expensemanager.data.Expense;
import com.example.expensemanager.data.ExpenseWithSource;
import com.example.expensemanager.data.Income;
import com.example.expensemanager.data.TransactionItem;
import com.example.expensemanager.data.ExpenseManagerDao;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ExpenseManagerDao dao;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance, tvCategoryBreakdown;
    private EditText etAmount, etSourceInput, etCategoryInput, etNote;
    private Spinner spTransactionType, spIncomeSources, spTimeFilter;
    private LinearLayout layoutExpenseSourceSelection;
    private Button btnPickDate, btnSaveTransaction;

    private RecyclerView rvTransactionHistory;
    private TransactionAdapter transactionAdapter;

    private List<Income> currentIncomes = new ArrayList<>();
    private List<ExpenseWithSource> currentExpenses = new ArrayList<>();
    
    private String selectedTransactionDate;
    private String activeFilterMode = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).expenseManagerDao();

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        
        spTransactionType = findViewById(R.id.spTransactionType);
        etAmount = findViewById(R.id.etAmount);
        etSourceInput = findViewById(R.id.etSourceInput);
        layoutExpenseSourceSelection = findViewById(R.id.layoutExpenseSourceSelection);
        spIncomeSources = findViewById(R.id.spIncomeSources);
        etCategoryInput = findViewById(R.id.etCategoryInput);
        etNote = findViewById(R.id.etNote);
        
        btnPickDate = findViewById(R.id.btnPickDate);
        spTimeFilter = findViewById(R.id.spTimeFilter);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);

        rvTransactionHistory = findViewById(R.id.rvTransactionHistory);
        rvTransactionHistory.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter();
        rvTransactionHistory.setAdapter(transactionAdapter);

        // ATTACH THE LONG PRESS ACTIONS INTERFACE CALLBACK
        transactionAdapter.setOnTransactionLongClickListener(new TransactionAdapter.OnTransactionLongClickListener() {
            @Override
            public void onEditSelected(TransactionItem item) {
                showEditTransactionDialog(item);
            }

            @Override
            public void onDeleteSelected(TransactionItem item) {
                showDeleteConfirmationDialog(item);
            }
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedTransactionDate = sdf.format(new Date());
        btnPickDate.setText("Selected Date: " + selectedTransactionDate);

        String[] timeFilters = {"All Time", "Today", "This Month"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, timeFilters);
        spTimeFilter.setAdapter(timeAdapter);

        String[] types = {"--Select--", "Income", "Expense"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spTransactionType.setAdapter(typeAdapter);

        btnPickDate.setOnClickListener(v -> showDatePickerWindow());
        
        spTimeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeFilterMode = timeFilters[position];
                calculateSummaryAndRefreshLogs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spTransactionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types[position];
                if (selectedType.equals("Income")) {
                    findViewById(R.id.inputLayoutAmount).setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutSource).setVisibility(View.VISIBLE);
                    layoutExpenseSourceSelection.setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutCategory).setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutNote).setVisibility(View.VISIBLE);
                    btnSaveTransaction.setVisibility(View.VISIBLE);
                } else if (selectedType.equals("Expense")) {
                    findViewById(R.id.inputLayoutAmount).setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutSource).setVisibility(View.GONE);
                    layoutExpenseSourceSelection.setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutCategory).setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutNote).setVisibility(View.VISIBLE);
                    btnSaveTransaction.setVisibility(View.VISIBLE);
                    populateIncomeSourceSpinner();
                } else {
                    findViewById(R.id.inputLayoutAmount).setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutSource).setVisibility(View.GONE);
                    layoutExpenseSourceSelection.setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutCategory).setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutNote).setVisibility(View.GONE);
                    btnSaveTransaction.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        dao.getAllIncome().observe(this, incomes -> {
            currentIncomes = incomes;
            if (spTransactionType.getSelectedItem().toString().equals("Expense")) {
                populateIncomeSourceSpinner();
            }
            calculateSummaryAndRefreshLogs();
        });

        dao.getAllExpensesWithSource().observe(this, expenses -> {
            currentExpenses = expenses;
            calculateSummaryAndRefreshLogs();
        });

        btnSaveTransaction.setOnClickListener(v -> handleTransactionProcessing());
    }

    private void showDatePickerWindow() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedTransactionDate = sdf.format(selected.getTime());
            btnPickDate.setText("Selected Date: " + selectedTransactionDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void populateIncomeSourceSpinner() {
        List<String> sourceOptions = new ArrayList<>();
        sourceOptions.add("--Select Source--");
        for (Income inc : currentIncomes) {
            sourceOptions.add(inc.source + " (₹" + inc.amount + ")");
        }
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sourceOptions);
        spIncomeSources.setAdapter(sourceAdapter);
    }

    private void handleTransactionProcessing() {
        String transactionType = spTransactionType.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();

        if (transactionType.equals("--Select--") || amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter valid transaction fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (transactionType.equals("Income")) {
            String sourceName = etSourceInput.getText().toString().trim();
            if (sourceName.isEmpty()) {
                Toast.makeText(this, "Please enter an income source.", Toast.LENGTH_SHORT).show();
                return;
            }
            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertIncome(new Income(amount, sourceName, selectedTransactionDate, "Online"));
            });
        } else if (transactionType.equals("Expense")) {
            int selectedSourceIndex = spIncomeSources.getSelectedItemPosition();
            String categoryName = etCategoryInput.getText().toString().trim();

            if (selectedSourceIndex == 0) {
                Toast.makeText(this, "Please select an active funding income source.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Please specify an expense category.", Toast.LENGTH_SHORT).show();
                return;
            }

            Income correspondingIncome = currentIncomes.get(selectedSourceIndex - 1);
            long linkedIncomeId = correspondingIncome.id;

            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertExpense(new Expense(amount, categoryName, linkedIncomeId, selectedTransactionDate, noteStr));
            });
        }

        etAmount.setText("");
        etSourceInput.setText("");
        etCategoryInput.setText("");
        etNote.setText("");
        spTransactionType.setSelection(0);
        Toast.makeText(this, "Transaction logged successfully!", Toast.LENGTH_SHORT).show();
    }

    // NEW: Action handler displaying database modification prompts
    private void showEditTransactionDialog(TransactionItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Transaction Entries");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputAmount = new EditText(this);
        inputAmount.setHint("Amount (₹)");
        inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputAmount.setText(String.valueOf(item.getAmount()));
        layout.addView(inputAmount);

        final EditText inputLabel = new EditText(this);
        inputLabel.setHint(item.isIncome() ? "Source Name" : "Category Name");
        inputLabel.setText(item.getTitle());
        layout.addView(inputLabel);

        builder.setView(layout);

        builder.setPositiveButton("Save Revisions", (dialog, which) -> {
            String amtStr = inputAmount.getText().toString().trim();
            String lblStr = inputLabel.getText().toString().trim();
            
            if(amtStr.isEmpty() || lblStr.isEmpty()) return;
            double amount = Double.parseDouble(amtStr);

            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (item.isIncome()) {
                    Income updatedIncome = new Income(amount, lblStr, item.getDate(), "Online");
                    updatedIncome.id = item.getId();
                    dao.updateIncome(updatedIncome);
                } else {
                    Expense updatedExpense = new Expense(amount, lblStr, item.getLinkedSourceId(), item.getDate(), item.getNote());
                    updatedExpense.id = item.getId();
                    dao.updateExpense(updatedExpense);
                }
            });
            Toast.makeText(MainActivity.this, "Transaction updated!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel Actions", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // NEW: Action handler displaying database removal prompts
    private void showDeleteConfirmationDialog(TransactionItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to permanently erase this transaction record entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (item.isIncome()) {
                            Income targetIncome = new Income(0,"","","");
                            targetIncome.id = item.getId();
                            dao.deleteIncome(targetIncome);
                        } else {
                            Expense targetExpense = new Expense(0,"",null,"","");
                            targetExpense.id = item.getId();
                            dao.deleteExpense(targetExpense);
                        }
                    });
                    Toast.makeText(MainActivity.this, "Transaction permanently removed.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void calculateSummaryAndRefreshLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        String currentMonthPrefix = todayStr.substring(0, 7);

        Map<Long, Double> sourceBalanceMap = new HashMap<>();
        for (Income inc : currentIncomes) {
            sourceBalanceMap.put(inc.id, inc.amount);
        }
        for (ExpenseWithSource exp : currentExpenses) {
            if (exp.expense.incomeSourceId != null && sourceBalanceMap.containsKey(exp.expense.incomeSourceId)) {
                double currentBal = sourceBalanceMap.get(exp.expense.incomeSourceId);
                sourceBalanceMap.put(exp.expense.incomeSourceId, currentBal - exp.expense.amount);
            }
        }

        double totalIncome = 0;
        List<TransactionItem> aggregatedItems = new ArrayList<>();

        for (Income inc : currentIncomes) {
            if (shouldIncludeInFilter(inc.date, todayStr, currentMonthPrefix)) {
                totalIncome += inc.amount;
                // Updated: Added database table entity id reference
                aggregatedItems.add(new TransactionItem(
                    inc.id, inc.source, inc.date, inc.amount, true, 
                    "Added directly into balance pools.", null, null, 0.0
                ));
            }
        }

        double totalExpense = 0;
        Map<String, Double> categoryMap = new HashMap<>();

        for (ExpenseWithSource exp : currentExpenses) {
            if (shouldIncludeInFilter(exp.expense.date, todayStr, currentMonthPrefix)) {
                totalExpense += exp.expense.amount;

                String cat = exp.expense.category;
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + exp.expense.amount);

                String srcName = exp.sourceName != null ? exp.sourceName : "Unlinked Pool";
                double availableSrcBal = 0.0;
                if (exp.expense.incomeSourceId != null && sourceBalanceMap.containsKey(exp.expense.incomeSourceId)) {
                    availableSrcBal = sourceBalanceMap.get(exp.expense.incomeSourceId);
                }

                // Updated: Added database entity id and tracking references
                aggregatedItems.add(new TransactionItem(
                    exp.expense.id, 
                    exp.expense.category, 
                    exp.expense.date, 
                    exp.expense.amount, 
                    false, 
                    exp.expense.note, 
                    srcName, 
                    exp.expense.incomeSourceId,
                    availableSrcBal
                ));
            }
        }

        Collections.sort(aggregatedItems, (item1, item2) -> item2.getDate().compareTo(item1.getDate()));
        transactionAdapter.updateData(aggregatedItems);

        double netBalance = totalIncome - totalExpense;

        tvTotalIncome.setText("₹" + totalIncome);
        tvTotalExpense.setText("₹" + totalExpense);
        tvNetBalance.setText("₹" + netBalance);

        if (categoryMap.isEmpty()) {
            tvCategoryBreakdown.setText("No expenses for this selected timeframe range.");
        } else {
            StringBuilder breakdown = new StringBuilder();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                breakdown.append(entry.getKey()).append(": ₹").append(entry.getValue()).append("  |  ");
            }
            tvCategoryBreakdown.setText(breakdown.toString());
        }
    }

    private boolean shouldIncludeInFilter(String transactionDate, String today, String currentMonthPrefix) {
        if (transactionDate == null) return false;
        switch (activeFilterMode) {
            case "Today":
                return transactionDate.equals(today);
            case "This Month":
                return transactionDate.startsWith(currentMonthPrefix);
            case "All Time":
            default:
                return true;
        }
    }
}
