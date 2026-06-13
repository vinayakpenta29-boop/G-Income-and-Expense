package com.example.expensemanager.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.expensemanager.R;
import com.example.expensemanager.data.AppDatabase;
import com.example.expensemanager.data.Expense;
import com.example.expensemanager.data.ExpenseWithSource;
import com.example.expensemanager.data.Income;
import com.example.expensemanager.data.ExpenseManagerDao;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ExpenseManagerDao dao;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance, tvLogsDisplay;
    private EditText etIncomeAmount, etIncomeSource, etExpenseAmount, etExpenseCategory, etExpenseNote;
    private CheckBox cbIsOnline;

    private List<Income> currentIncomes = new ArrayList<>();
    private List<ExpenseWithSource> currentExpenses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).expenseManagerDao();

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvLogsDisplay = findViewById(R.id.tvLogsDisplay);
        
        etIncomeAmount = findViewById(R.id.etIncomeAmount);
        etIncomeSource = findViewById(R.id.etIncomeSource);
        cbIsOnline = findViewById(R.id.cbIsOnline);
        
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseCategory = findViewById(R.id.etExpenseCategory);
        etExpenseNote = findViewById(R.id.etExpenseNote);

        Button btnSaveIncome = findViewById(R.id.btnSaveIncome);
        Button btnSaveExpense = findViewById(R.id.btnSaveExpense);

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

    private void saveIncomeToDatabase() {
        String amountStr = etIncomeAmount.getText().toString().trim();
        String source = etIncomeSource.getText().toString().trim();
        if (amountStr.isEmpty() || source.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);
        String method = cbIsOnline.isChecked() ? "Online" : "Cash";

        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insertIncome(new Income(amount, source, "2026-06-13", method));
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
            dao.insertExpense(new Expense(amount, category, finalTargetedIncomeId, "2026-06-13", note));
        });

        etExpenseAmount.setText("");
        etExpenseCategory.setText("");
        etExpenseNote.setText("");
    }

    private void calculateSummaryAndRefreshLogs() {
        double totalIncome = 0;
        for (Income inc : currentIncomes) {
            totalIncome += inc.amount;
        }

        double totalExpense = 0;
        for (ExpenseWithSource exp : currentExpenses) {
            totalExpense += exp.expense.amount;
        }

        double netBalance = totalIncome - totalExpense;

        tvTotalIncome.setText("Total Income: $" + totalIncome);
        tvTotalExpense.setText("Total Expenses: $" + totalExpense);
        tvNetBalance.setText("Net Balance: $" + netBalance);

        StringBuilder logs = new StringBuilder();
        logs.append("--- Recent Expense Logs ---\n");
        for (ExpenseWithSource ews : currentExpenses) {
            String linkInfo = ews.sourceName != null ? " [Funded by: " + ews.sourceName + "]" : " [Funded by: Unknown]";
            logs.append("• ").append(ews.expense.category)
                    .append(": $").append(ews.expense.amount)
                    .append(linkInfo).append("\n");
        }
        tvLogsDisplay.setText(logs.toString());
    }
}
