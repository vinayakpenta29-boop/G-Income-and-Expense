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
    private EditText etAmount, etLabel, etNote;
    private RadioGroup rgTransactionType;
    private RadioButton rbIncome;
    private Button btnPickDate;
    private Spinner spTimeFilter;

    private List<Income> currentIncomes = new ArrayList<>();
    private List<ExpenseWithSource> currentExpenses = new ArrayList<>();
    
    private String selectedTransactionDate;
    private String activeFilterMode = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).expenseManagerDao();

        // Bind Unified Layout Views
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        tvLogsDisplay = findViewById(R.id.tvLogsDisplay);
        
        rgTransactionType = findViewById(R.id.rgTransactionType);
        rbIncome = findViewById(R.id.rbIncome);
        etAmount = findViewById(R.id.etAmount);
        etLabel = findViewById(R.id.etLabel);
        etNote = findViewById(R.id.etNote);
        
        btnPickDate = findViewById(R.id.btnPickDate);
        spTimeFilter = findViewById(R.id.spTimeFilter);
        Button btnSaveTransaction = findViewById(R.id.btnSaveTransaction);

        // Initialize default timestamp format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedTransactionDate = sdf.format(new Date());
        btnPickDate.setText("Selected Date: " + selectedTransactionDate);

        // Setup drop-down array structure for periods
        String[] filters = {"All Time", "Today", "This Month"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spTimeFilter.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> showDatePickerWindow());
        
        spTimeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeFilterMode = filters[position];
                calculateSummaryAndRefreshLogs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Room Observers
        dao.getAllIncome().observe(this, incomes -> {
            currentIncomes = incomes;
            calculateSummaryAndRefreshLogs();
        });

        dao.getAllExpensesWithSource().observe(this, expenses -> {
            currentExpenses = expenses;
            calculateSummaryAndRefreshLogs();
        });

        // Unified Click Listener Save Entry Action
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

    private void handleTransactionProcessing() {
        String amountStr = etAmount.getText().toString().trim();
        String labelInput = etLabel.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();

        if (amountStr.isEmpty() || labelInput.isEmpty()) {
            Toast.makeText(this, "Please enter amount and label details.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // Branching Execution routing based on selection state
        if (rbIncome.isChecked()) {
            // Process as Income
            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertIncome(new Income(amount, labelInput, selectedTransactionDate, "Online"));
            });
        } else {
            // Process as Expense
            Long targetedIncomeId = null;
            if (!currentIncomes.isEmpty()) {
                targetedIncomeId = currentIncomes.get(0).id; // Automatically links to latest income source
            }

            Long finalTargetedIncomeId = targetedIncomeId;
            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertExpense(new Expense(amount, labelInput, finalTargetedIncomeId, selectedTransactionDate, noteStr));
            });
        }

        // Wipe selection states clear
        etAmount.setText("");
        etLabel.setText("");
        etNote.setText("");
        Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
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

        // Add Incomes to history view logs list output display string console
        for (Income inc : currentIncomes) {
            if (shouldIncludeInFilter(inc.date, todayStr, currentMonthPrefix)) {
                logs.append("➕ [").append(inc.date).append("] ")
                    .append(inc.source).append(": +$").append(inc.amount).append("\n");
            }
        }

        // Add Expenses to history view logs list output display string console
        for (ExpenseWithSource exp : currentExpenses) {
            if (shouldIncludeInFilter(exp.expense.date, todayStr, currentMonthPrefix)) {
                totalExpense += exp.expense.amount;

                String cat = exp.expense.category;
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + exp.expense.amount);

                logs.append("➖ [").append(exp.expense.date).append("] ")
                    .append(exp.expense.category).append(": -$").append(exp.expense.amount).append("\n");
            }
        }

        double netBalance = totalIncome - totalExpense;

        tvTotalIncome.setText("Total Income: $" + totalIncome);
        tvTotalExpense.setText("Total Expenses: $" + totalExpense);
        tvNetBalance.setText("Net Balance: $" + netBalance);
        tvLogsDisplay.setText(logs.toString());

        if (categoryMap.isEmpty()) {
            tvCategoryBreakdown.setText("No expenses for this selected timeframe selection range.");
        } else {
            StringBuilder breakdown = new StringBuilder();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                breakdown.append(entry.getKey()).append(": $").append(entry.getValue()).append("  |  ");
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
