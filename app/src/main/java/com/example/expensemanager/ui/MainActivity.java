package com.example.expensemanager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.expensemanager.R;
import com.example.expensemanager.data.AppDatabase;
import com.example.expensemanager.data.Expense;
import com.example.expensemanager.data.ExpenseWithSource;
import com.example.expensemanager.data.Income;
import com.example.expensemanager.data.ExpenseManagerDao;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ExpenseManagerDao dao;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance, tvLogsDisplay, tvCategoryBreakdown;
    private EditText etAmount, etSourceInput, etCategoryInput, etNote;
    private Spinner spTransactionType, spIncomeSources, spTimeFilter;
    private LinearLayout layoutExpenseSourceSelection;
    private Button btnPickDate, btnSaveTransaction;

    private List<Income> currentIncomes = new ArrayList<>();
    private List<ExpenseWithSource> currentExpenses = new ArrayList<>();
    
    private String selectedTransactionDate;
    private String activeFilterMode = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).expenseManagerDao();

        // Bind View Components
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        tvLogsDisplay = findViewById(R.id.tvLogsDisplay);
        
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

        // Date Initializations
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedTransactionDate = sdf.format(new Date());
        btnPickDate.setText("Selected Date: " + selectedTransactionDate);

        // Setup Main Time Period Filter Spinner
        String[] timeFilters = {"All Time", "Today", "This Month"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, timeFilters);
        spTimeFilter.setAdapter(timeAdapter);

        // Setup Transaction Type Selector with standard default placeholder
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

        // Form fields toggle logic based on selection state
        spTransactionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types[position];
                if (selectedType.equals("Income")) {
                    etAmount.setVisibility(View.VISIBLE);
                    etSourceInput.setVisibility(View.VISIBLE);
                    layoutExpenseSourceSelection.setVisibility(View.GONE);
                    etCategoryInput.setVisibility(View.GONE);
                    etNote.setVisibility(View.VISIBLE);
                    btnSaveTransaction.setVisibility(View.VISIBLE);
                } else if (selectedType.equals("Expense")) {
                    etAmount.setVisibility(View.VISIBLE);
                    etSourceInput.setVisibility(View.GONE);
                    layoutExpenseSourceSelection.setVisibility(View.VISIBLE);
                    etCategoryInput.setVisibility(View.VISIBLE);
                    etNote.setVisibility(View.VISIBLE);
                    btnSaveTransaction.setVisibility(View.VISIBLE);
                    populateIncomeSourceSpinner(); // Refresh targets
                } else {
                    // "--Select--" configuration state resets form layout views
                    etAmount.setVisibility(View.GONE);
                    etSourceInput.setVisibility(View.GONE);
                    layoutExpenseSourceSelection.setVisibility(View.GONE);
                    etCategoryInput.setVisibility(View.GONE);
                    etNote.setVisibility(View.GONE);
                    btnSaveTransaction.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Database Pipelines Observers
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

            if (selectedSourceIndex == 0) { // Points to "--Select Source--"
                Toast.makeText(this, "Please select an active funding income source.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Please specify an expense category.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Maps index choice minus placeholder header directly to local list structure array elements
            Income correspondingIncome = currentIncomes.get(selectedSourceIndex - 1);
            long linkedIncomeId = correspondingIncome.id;

            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertExpense(new Expense(amount, categoryName, linkedIncomeId, selectedTransactionDate, noteStr));
            });
        }

        // Clean input fields and reset view components
        etAmount.setText("");
        etSourceInput.setText("");
        etCategoryInput.setText("");
        etNote.setText("");
        spTransactionType.setSelection(0); // Return view states safely back to placeholder defaults
        Toast.makeText(this, "Transaction logged successfully!", Toast.LENGTH_SHORT).show();
    }

    private void calculateSummaryAndRefreshLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        String currentMonthPrefix = todayStr.substring(0, 7);

        double totalIncome = 0;
        for (Income inc : currentIncomes) {
            if (shouldIncludeInFilter(inc.date, todayStr, currentMonthPrefix)) {
                totalIncome += inc.amount;
            }
        }

        double totalExpense = 0;
        Map<String, Double> categoryMap = new HashMap<>();
        StringBuilder logs = new StringBuilder();
        
        logs.append("--- Unified Consolidated History Logs ---\n");

        // Formats income entries to show the income source name
        for (Income inc : currentIncomes) {
            if (shouldIncludeInFilter(inc.date, todayStr, currentMonthPrefix)) {
                logs.append("➕ [").append(inc.date).append("] ")
                    .append(inc.source).append(": +₹").append(inc.amount).append("\n");
            }
        }

        // Formats expense entries to show the expense category name
        for (ExpenseWithSource exp : currentExpenses) {
            if (shouldIncludeInFilter(exp.expense.date, todayStr, currentMonthPrefix)) {
                totalExpense += exp.expense.amount;

                String cat = exp.expense.category;
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + exp.expense.amount);

                logs.append("➖ [").append(exp.expense.date).append("] ")
                    .append(exp.expense.category).append(": -₹").append(exp.expense.amount).append("\n");
            }
        }

        double netBalance = totalIncome - totalExpense;

        // Render calculations using the Indian Rupee symbol
        tvTotalIncome.setText("Total Income: ₹" + totalIncome);
        tvTotalExpense.setText("Total Expenses: ₹" + totalExpense);
        tvNetBalance.setText("Net Balance: ₹" + netBalance);
        tvLogsDisplay.setText(logs.toString());

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
