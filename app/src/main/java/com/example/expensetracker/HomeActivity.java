package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeActivity extends AppCompatActivity {

    private FloatingActionButton btnOpenCalc, btnOpenCurrency, fabAddExpense;
    private TextView tvTotalAmount;
    private PieChart pieChart;

    private RecyclerView rvExpenses;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList;

    // FIXED: Using Firestore instead of DatabaseReference
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String TAG = "FIRESTORE_DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        // FIXED: Initialize the Firestore instance here
        db = FirebaseFirestore.getInstance();

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        pieChart = findViewById(R.id.pieChart);
        btnOpenCalc = findViewById(R.id.btnOpenCalc);
        btnOpenCurrency = findViewById(R.id.btnOpenCurrency);
        fabAddExpense = findViewById(R.id.fabAddExpense);

        rvExpenses = findViewById(R.id.rvExpenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        expenseList = new ArrayList<>();
        adapter = new ExpenseAdapter(expenseList, position -> deleteExpense(position));
        rvExpenses.setAdapter(adapter);

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
            String amountStr = etAmount.getText().toString().trim();
            String category = spCategory.getSelectedItem().toString();
            String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(calendar.getTime());

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (userId == null) return;

            // FIXED: Corrected Firestore Reference (Collection -> Document -> Collection)
            CollectionReference userExpenses = db.collection("Users").document(userId).collection("Expenses");
            DocumentReference newDoc = userExpenses.document();

            Expense expense = new Expense(newDoc.getId(), title, category, amountStr, formattedDate);

            newDoc.set(expense)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HomeActivity.this, "Saved to Firestore", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Save Error: " + e.getMessage()));
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
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // FIXED: Changed from Realtime Database listener to Firestore listener
        db.collection("Users").document(userId).collection("Expenses")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        expenseList.clear();
                        double total = 0;
                        Map<String, Float> categoryMap = new HashMap<>();

                        for (QueryDocumentSnapshot doc : value) {
                            Expense expense = doc.toObject(Expense.class);
                            expenseList.add(expense);
                            try {
                                float amt = Float.parseFloat(expense.amount);
                                total += amt;
                                categoryMap.put(expense.category, categoryMap.getOrDefault(expense.category, 0f) + amt);
                            } catch (Exception e) {
                                Log.e(TAG, "Parsing error");
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvTotalAmount.setText("₹" + String.format("%.2f", total));
                        updateChart(categoryMap);
                    }
                });
    }

    private void updateChart(Map<String, Float> categoryMap) {
        if (categoryMap.isEmpty()) {
            pieChart.clear();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void deleteExpense(int position) {
        if (position >= expenseList.size()) return;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (d, which) -> {
                    String id = expenseList.get(position).id;
                    String userId = mAuth.getCurrentUser().getUid();

                    // FIXED: Using Firestore delete logic
                    db.collection("Users").document(userId).collection("Expenses").document(id)
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e(TAG, "Delete failed: " + e.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .create();

        // (The CountDownTimer logic you had is correct and remains the same)
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