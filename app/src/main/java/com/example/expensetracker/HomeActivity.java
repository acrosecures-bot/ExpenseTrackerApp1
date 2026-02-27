package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeActivity extends AppCompatActivity {

    private FloatingActionButton btnOpenCalc, btnOpenCurrency, fabAddExpense;
    private TextView tvTotalAmount;
    private PieChart pieChart;

    private RecyclerView rvExpenses;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Expenses");

        // UI Initialization
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        pieChart = findViewById(R.id.pieChart);
        btnOpenCalc = findViewById(R.id.btnOpenCalc);
        btnOpenCurrency = findViewById(R.id.btnOpenCurrency);
        fabAddExpense = findViewById(R.id.fabAddExpense);

        // RecyclerView Setup
        rvExpenses = findViewById(R.id.rvExpenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        expenseList = new ArrayList<>();
        adapter = new ExpenseAdapter(expenseList, position -> deleteExpense(position));
        rvExpenses.setAdapter(adapter);

        // Click Listeners
        btnOpenCalc.setOnClickListener(v -> startActivity(new Intent(this, CalculatorActivity.class)));
        btnOpenCurrency.setOnClickListener(v -> startActivity(new Intent(this, CurrencyActivity.class)));
        fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        setupPieChart();
        loadData();
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);
        builder.setView(view);

        EditText etTitle = view.findViewById(R.id.etExpenseTitle);
        EditText etAmount = view.findViewById(R.id.etExpenseAmount);
        Spinner spCategory = view.findViewById(R.id.spCategory);
        Button btnDatePicker = view.findViewById(R.id.btnDatePicker);
        Button btnTimePicker = view.findViewById(R.id.btnTimePicker);
        Button btnSave = view.findViewById(R.id.btnAddExpenseConfirm);

        String[] categories = {"Food", "Shopping", "Transport", "Rent", "Bills", "Other"};
        spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories));

        final Calendar calendar = Calendar.getInstance();
        
        btnDatePicker.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDatePicker.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTimePicker.setOnClickListener(v -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                btnTimePicker.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String amount = etAmount.getText().toString().trim();
            String category = spCategory.getSelectedItem().toString();
            String dateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(calendar.getTime());

            if (title.isEmpty() || amount.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = mDatabase.push().getKey();
            Expense expense = new Expense(id, title, category, amount, dateTime);
            mDatabase.child(mAuth.getCurrentUser().getUid()).child(id).setValue(expense);
            
            Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawEntryLabels(false);
        pieChart.animateY(1400);
    }

    private void loadData() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expenseList.clear();
                double total = 0;
                Map<String, Float> categoryMap = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Expense expense = data.getValue(Expense.class);
                    if (expense != null) {
                        expenseList.add(expense);
                        try {
                            float amt = Float.parseFloat(expense.amount);
                            total += amt;
                            categoryMap.put(expense.category, categoryMap.getOrDefault(expense.category, 0f) + amt);
                        } catch (Exception e) {}
                    }
                }
                adapter.notifyDataSetChanged();
                tvTotalAmount.setText("₹" + String.format("%.2f", total));
                updateChart(categoryMap);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateChart(Map<String, Float> categoryMap) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(android.graphics.Color.BLACK);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void deleteExpense(int position) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (d, which) -> {
                    String id = expenseList.get(position).id;
                    mDatabase.child(mAuth.getCurrentUser().getUid()).child(id).removeValue();
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .create();

        CountDownTimer timer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (deleteButton != null) {
                    deleteButton.setText("Delete (" + (millisUntilFinished / 1000 + 1) + ")");
                }
            }

            @Override
            public void onFinish() {
                Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (deleteButton != null) {
                    deleteButton.setEnabled(true);
                    deleteButton.setText("Delete");
                }
            }
        };

        dialog.setOnShowListener(dialogInterface -> {
            Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            deleteButton.setEnabled(false);
            timer.start();
        });

        dialog.setOnDismissListener(dialogInterface -> timer.cancel());

        dialog.show();
    }
}