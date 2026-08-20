package com.example.expensemanager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensemanager.R;
import com.example.expensemanager.data.AccountSource;
import com.example.expensemanager.data.AppDatabase;
import com.example.expensemanager.data.Expense;
import com.example.expensemanager.data.Income;
import com.example.expensemanager.data.TransactionItem;
import com.example.expensemanager.data.ExpenseManagerDao;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ExpenseManagerDao dao;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance, tvCategoryBreakdown, tvSourcesBreakdown;
    private EditText etAmount, etTitleInput, etNote;
    private Spinner spTransactionType, spAccountSource, spTimeFilter;
    private LinearLayout layoutSourceSelection;
    private Button btnPickDate, btnSaveTransaction;
    private MaterialToolbar topAppBar;

    private RecyclerView rvTransactionHistory;
    private TransactionAdapter transactionAdapter;

    private List<AccountSource> currentSources = new ArrayList<>();
    private List<Income> currentIncomes = new ArrayList<>();
    private List<Expense> currentExpenses = new ArrayList<>();
    
    private String selectedTransactionDate;
    private String activeFilterMode = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).expenseManagerDao();

        // Bind Toolbar & 3-Dots Menu
        topAppBar = findViewById(R.id.topAppBar);
        topAppBar.inflateMenu(R.menu.main_menu);
        topAppBar.setOnMenuItemClickListener(this::onToolbarMenuItemClicked);

        // Bind Views
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvSourcesBreakdown = findViewById(R.id.tvSourcesBreakdown);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        
        spTransactionType = findViewById(R.id.spTransactionType);
        etAmount = findViewById(R.id.etAmount);
        etTitleInput = findViewById(R.id.etTitleInput);
        layoutSourceSelection = findViewById(R.id.layoutSourceSelection);
        spAccountSource = findViewById(R.id.spAccountSource);
        etNote = findViewById(R.id.etNote);
        
        btnPickDate = findViewById(R.id.btnPickDate);
        spTimeFilter = findViewById(R.id.spTimeFilter);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);

        rvTransactionHistory = findViewById(R.id.rvTransactionHistory);
        rvTransactionHistory.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter();
        rvTransactionHistory.setAdapter(transactionAdapter);

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
                    findViewById(R.id.inputLayoutTitle).setVisibility(View.GONE);
                    layoutSourceSelection.setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutNote).setVisibility(View.VISIBLE);
                    btnSaveTransaction.setVisibility(View.VISIBLE);
                    populateAccountSourceSpinner();
                } else if (selectedType.equals("Expense")) {
                    findViewById(R.id.inputLayoutAmount).setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutTitle).setVisibility(View.VISIBLE);
                    ((com.google.android.material.textfield.TextInputLayout)findViewById(R.id.inputLayoutTitle)).setHint("Expense Category (e.g. Food)");
                    layoutSourceSelection.setVisibility(View.VISIBLE);
                    findViewById(R.id.inputLayoutNote).setVisibility(View.VISIBLE);
                    btnSaveTransaction.setVisibility(View.VISIBLE);
                    populateAccountSourceSpinner();
                } else {
                    findViewById(R.id.inputLayoutAmount).setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutTitle).setVisibility(View.GONE);
                    layoutSourceSelection.setVisibility(View.GONE);
                    findViewById(R.id.inputLayoutNote).setVisibility(View.GONE);
                    btnSaveTransaction.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Observers
        dao.getAllSources().observe(this, sources -> {
            currentSources = sources;
            populateAccountSourceSpinner();
            calculateSummaryAndRefreshLogs();
        });

        dao.getAllIncome().observe(this, incomes -> {
            currentIncomes = incomes;
            populateAccountSourceSpinner();
            calculateSummaryAndRefreshLogs();
        });

        dao.getAllExpenses().observe(this, expenses -> {
            currentExpenses = expenses;
            populateAccountSourceSpinner();
            calculateSummaryAndRefreshLogs();
        });

        btnSaveTransaction.setOnClickListener(v -> handleTransactionProcessing());
    }

    private boolean onToolbarMenuItemClicked(MenuItem item) {
        if (item.getItemId() == R.id.action_sources) {
            showManageSourcesDialog();
            return true;
        }
        return false;
    }

    private void showManageSourcesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Account Sources");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 10);

        TextView info = new TextView(this);
        info.setText("Available Sources (Click to delete):");
        info.setTextSize(14);
        info.setPadding(0, 0, 0, 10);
        container.addView(info);

        for (AccountSource src : currentSources) {
            Button btnSrc = new Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnSrc.setText(src.name + " ✕");
            btnSrc.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Source")
                        .setMessage("Delete '" + src.name + "'? Existing transactions linked to this source will remain.")
                        .setPositiveButton("Delete", (d, w) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> dao.deleteSource(src));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            container.addView(btnSrc);
        }

        Button btnAdd = new Button(this);
        btnAdd.setText("+ Add New Source");
        btnAdd.setOnClickListener(v -> showAddNewSourceDialog());
        container.addView(btnAdd);

        builder.setView(container);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showAddNewSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Source Account");

        final EditText input = new EditText(this);
        input.setHint("Source Name (e.g. SBI Bank, Cash, Paytm)");
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(40, 20, 40, 10);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                AppDatabase.databaseWriteExecutor.execute(() -> dao.insertSource(new AccountSource(name)));
                Toast.makeText(this, "Source '" + name + "' added!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private Map<Long, Double> computeSourceBalances() {
        Map<Long, Double> map = new HashMap<>();
        for (AccountSource s : currentSources) {
            map.put(s.id, 0.0);
        }
        for (Income inc : currentIncomes) {
            if (inc.sourceId != null && map.containsKey(inc.sourceId)) {
                map.put(inc.sourceId, map.get(inc.sourceId) + inc.amount);
            }
        }
        for (Expense exp : currentExpenses) {
            if (exp.sourceId != null && map.containsKey(exp.sourceId)) {
                map.put(exp.sourceId, map.get(exp.sourceId) - exp.amount);
            }
        }
        return map;
    }

    private void populateAccountSourceSpinner() {
        Map<Long, Double> balances = computeSourceBalances();
        List<String> options = new ArrayList<>();
        options.add("--Select Source--");
        for (AccountSource src : currentSources) {
            double bal = balances.getOrDefault(src.id, 0.0);
            options.add(src.name + " (₹" + String.format(Locale.US, "%.2f", bal) + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        spAccountSource.setAdapter(adapter);
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
        String transactionType = spTransactionType.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();
        int selectedSourcePos = spAccountSource.getSelectedItemPosition();

        if (transactionType.equals("--Select--") || amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSourcePos == 0 || currentSources.isEmpty()) {
            Toast.makeText(this, "Please select an account/source.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        AccountSource selectedSource = currentSources.get(selectedSourcePos - 1);

        if (transactionType.equals("Income")) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertIncome(new Income(amount, selectedSource.name, selectedSource.id, selectedSource.name, selectedTransactionDate, "Online", noteStr));
            });
        } else if (transactionType.equals("Expense")) {
            String categoryStr = etTitleInput.getText().toString().trim();
            if (categoryStr.isEmpty()) {
                Toast.makeText(this, "Please enter an expense category.", Toast.LENGTH_SHORT).show();
                return;
            }
            AppDatabase.databaseWriteExecutor.execute(() -> {
                dao.insertExpense(new Expense(amount, categoryStr, selectedSource.id, selectedSource.name, selectedTransactionDate, noteStr));
            });
        }

        etAmount.setText("");
        etTitleInput.setText("");
        etNote.setText("");
        spTransactionType.setSelection(0);
        Toast.makeText(this, "Transaction logged successfully!", Toast.LENGTH_SHORT).show();
    }

    // UPDATED: Added Source Selection Dropdown Spinner in Edit Dialog
    private void showEditTransactionDialog(TransactionItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.isIncome() ? "Edit Income Transaction" : "Edit Expense Transaction");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // Amount Input
        final EditText inputAmount = new EditText(this);
        inputAmount.setHint("Amount (₹)");
        inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputAmount.setText(String.format(Locale.US, "%.2f", item.getAmount()));
        layout.addView(inputAmount);

        // Expense Category Input (Only for Expenses)
        final EditText inputTitle = new EditText(this);
        if (!item.isIncome()) {
            inputTitle.setHint("Expense Category (e.g. Food)");
            inputTitle.setText(item.getTitle());
            layout.addView(inputTitle);
        }

        // Account Source Label
        TextView tvSourceLabel = new TextView(this);
        tvSourceLabel.setText("Account / Source:");
        tvSourceLabel.setTextSize(13);
        tvSourceLabel.setPadding(0, 16, 0, 8);
        layout.addView(tvSourceLabel);

        // Account Source Spinner
        final Spinner spEditSource = new Spinner(this);
        List<String> sourceNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < currentSources.size(); i++) {
            AccountSource src = currentSources.get(i);
            sourceNames.add(src.name);
            if (item.getSourceId() != null && src.id == item.getSourceId()) {
                selectedIndex = i;
            }
        }
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sourceNames);
        spEditSource.setAdapter(sourceAdapter);
        if (!sourceNames.isEmpty()) {
            spEditSource.setSelection(selectedIndex);
        }
        layout.addView(spEditSource);

        // Note Input
        final EditText inputNote = new EditText(this);
        inputNote.setHint("Note / Description (Optional)");
        inputNote.setText(item.getNote() != null ? item.getNote() : "");
        layout.addView(inputNote);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String amtStr = inputAmount.getText().toString().trim();
            if (amtStr.isEmpty()) return;

            double amount = Double.parseDouble(amtStr);
            String note = inputNote.getText().toString().trim();

            AccountSource selectedSource = currentSources.isEmpty() ? null : currentSources.get(spEditSource.getSelectedItemPosition());
            Long newSourceId = selectedSource != null ? selectedSource.id : item.getSourceId();
            String newSourceName = selectedSource != null ? selectedSource.name : item.getSourceName();

            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (item.isIncome()) {
                    String title = newSourceName != null ? newSourceName : "Income";
                    Income inc = new Income(amount, title, newSourceId, newSourceName, item.getDate(), "Online", note);
                    inc.id = item.getId();
                    dao.updateIncome(inc);
                } else {
                    String title = inputTitle.getText().toString().trim();
                    if (title.isEmpty()) title = item.getTitle();
                    Expense exp = new Expense(amount, title, newSourceId, newSourceName, item.getDate(), note);
                    exp.id = item.getId();
                    dao.updateExpense(exp);
                }
            });
            Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(TransactionItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Delete this record permanently?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (item.isIncome()) {
                            Income inc = new Income(0,"",null,"","","","");
                            inc.id = item.getId();
                            dao.deleteIncome(inc);
                        } else {
                            Expense exp = new Expense(0,"",null,"","","");
                            exp.id = item.getId();
                            dao.deleteExpense(exp);
                        }
                    });
                    Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void calculateSummaryAndRefreshLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        String currentMonthPrefix = todayStr.substring(0, 7);

        Map<Long, Double> sourceBalances = computeSourceBalances();

        double totalIncome = 0;
        List<TransactionItem> aggregatedItems = new ArrayList<>();

        for (Income inc : currentIncomes) {
            if (shouldIncludeInFilter(inc.date, todayStr, currentMonthPrefix)) {
                totalIncome += inc.amount;
                double availBal = inc.sourceId != null ? sourceBalances.getOrDefault(inc.sourceId, 0.0) : 0.0;
                String displayTitle = inc.sourceName != null ? inc.sourceName : "Income";
                aggregatedItems.add(new TransactionItem(
                    inc.id, displayTitle, inc.date, inc.amount, true, 
                    inc.note, inc.sourceId, inc.sourceName, availBal
                ));
            }
        }

        double totalExpense = 0;
        Map<String, Double> categoryMap = new HashMap<>();

        for (Expense exp : currentExpenses) {
            if (shouldIncludeInFilter(exp.date, todayStr, currentMonthPrefix)) {
                totalExpense += exp.amount;
                categoryMap.put(exp.category, categoryMap.getOrDefault(exp.category, 0.0) + exp.amount);

                double availBal = exp.sourceId != null ? sourceBalances.getOrDefault(exp.sourceId, 0.0) : 0.0;
                aggregatedItems.add(new TransactionItem(
                    exp.id, exp.category, exp.date, exp.amount, false, 
                    exp.note, exp.sourceId, exp.sourceName, availBal
                ));
            }
        }

        Collections.sort(aggregatedItems, (item1, item2) -> item2.getDate().compareTo(item1.getDate()));
        transactionAdapter.updateData(aggregatedItems);

        double netBalance = totalIncome - totalExpense;

        tvTotalIncome.setText("₹" + String.format(Locale.US, "%.2f", totalIncome));
        tvTotalExpense.setText("₹" + String.format(Locale.US, "%.2f", totalExpense));
        tvNetBalance.setText("₹" + String.format(Locale.US, "%.2f", netBalance));

        if (currentSources.isEmpty()) {
            tvSourcesBreakdown.setText("Sources: No sources created yet.");
        } else {
            StringBuilder sb = new StringBuilder("Sources:  ");
            for (AccountSource s : currentSources) {
                double b = sourceBalances.getOrDefault(s.id, 0.0);
                sb.append(s.name).append(": ₹").append(String.format(Locale.US, "%.2f", b)).append("   |   ");
            }
            String result = sb.toString();
            if (result.endsWith("   |   ")) {
                result = result.substring(0, result.length() - 7);
            }
            tvSourcesBreakdown.setText(result);
        }

        if (categoryMap.isEmpty()) {
            tvCategoryBreakdown.setText("No expenses for this selected timeframe range.");
        } else {
            StringBuilder breakdown = new StringBuilder();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                breakdown.append(entry.getKey()).append(": ₹")
                         .append(String.format(Locale.US, "%.2f", entry.getValue())).append("  |  ");
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
