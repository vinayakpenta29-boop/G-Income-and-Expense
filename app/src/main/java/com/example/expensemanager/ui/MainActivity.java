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
    private EditText etIncomeAmount, etIncomeSource, etExpenseAmount, etExpenseCategory, etExpenseNote;
    private CheckBox cbIsOnline;
    private Button btnPickDate;
    private Spinner spTimeFilter;

    private List<Income> currentIncomes = new ArrayList<>();
    private List<ExpenseWithSource> currentExpenses = new ArrayList<>();
    
    // Tracks state variables
    private String selectedTransactionDate;
    private String activeFilterMode = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).expenseManagerDao();

        // Bind View Elements
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        tvLogsDisplay = findViewById(R.id.tvLogsDisplay);
        
        etIncomeAmount = findViewById(R.id.etIncomeAmount);
        etIncomeSource = findViewById(R.id.etIncomeSource);
        cbIsOnline = findViewById(R.id.cbIsOnline);
        
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseCategory = findViewById(R.id.etExpenseCategory);
        etExpenseNote = findViewById(R.id.etExpenseNote);
        btnPickDate = findViewById(R.id.btnPickDate);
        spTimeFilter = findViewById(R.id.spTimeFilter);

        Button btnSaveIncome = findViewById(R.id.btnSaveIncome);
        Button btnSaveExpense = findViewById(R.id.btnSaveExpense);

        // Initialize default runtime date to current date system format (YYYY-MM-DD)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedTransactionDate = sdf.format(new Date());
        btnPickDate.setText("Selected Date: " + selectedTransactionDate);

        // Setup drop-down list array structure for period filtration
        String[] filters = {"All Time", "Today", "This Month"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spTimeFilter.setAdapter(adapter);

        // Event hooks
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

        btnSaveIncome.setOnClickListener(v -> saveIncomeToDatabase());
        btnSaveExpense.setOnClickListener(v -> saveExpenseToDatabase());
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

    private void saveIncomeToDatabase() {
        String amountStr = etIncomeAmount.getText().toString().trim();
        String source = etIncomeSource.getText().toString().trim();
        if (amountStr.isEmpty() || source.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);
        String method = cbIsOnline.isChecked() ? "Online" : "Cash";

        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insertIncome(new Income(amount, source, selectedTransactionDate, method));
        });

        etIncomeAmount.setText("");
        etIncomeSource.setText("");
    }

    private void saveExpenseToDatabase() {
        String amountStr = etExpenseAmount.getText().toString().trim();
        String category = etExpenseCategory.getText().toString().trim();
        String note = etExpenseNote.getText().toString().trim();
        if (amountStr.isEmpty() || category.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);

        Long targetedIncomeId = null;
        if (!currentIncomes.isEmpty()) {
            targetedIncomeId = currentIncomes.get(0).id;
        }

        Long finalTargetedIncomeId = targetedIncomeId;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insertExpense(new Expense(amount, category, finalTargetedIncomeId, selectedTransactionDate, note));
        });

        etExpenseAmount.setText("");
        etExpenseCategory.setText("");
        etExpenseNote.setText("");
    }

    private void calculateSummaryAndRefreshLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        String currentMonthPrefix = todayStr.substring(0, 7); // Extracts "YYYY-MM"

        double totalIncome = 0;
        for (Income inc : currentIncomes) {
            if (shouldIncludeInFilter(inc.date, todayStr, currentMonthPrefix)) {
                totalIncome += inc.amount;
            }
        }

        double totalExpense = 0;
        Map<String, Double> categoryMap = new HashMap<>();
        StringBuilder logs = new StringBuilder();
        logs.append("--- Recent Expense Logs ---\n");

        for (ExpenseWithSource exp : currentExpenses) {
            if (shouldIncludeInFilter(exp.expense.date, todayStr, currentMonthPrefix)) {
                totalExpense += exp.expense.amount;

                // Group mapping logic calculation metrics
                String cat = exp.expense.category;
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + exp.expense.amount);

                String linkInfo = exp.sourceName != null ? " [Funded by: " + exp.sourceName + "]" : " [Funded by: Unknown]";
                logs.append("• [").append(exp.expense.date).append("] ")
                        .append(exp.expense.category).append(": $").append(exp.expense.amount)
                        .append(linkInfo).append("\n");
            }
        }

        double netBalance = totalIncome - totalExpense;

        // Render standard calculations
        tvTotalIncome.setText("Total Income: $" + totalIncome);
        tvTotalExpense.setText("Total Expenses: $" + totalExpense);
        tvNetBalance.setText("Net Balance: $" + netBalance);
        tvLogsDisplay.setText(logs.toString());

        // Render Spending Breakdown Calculations
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
