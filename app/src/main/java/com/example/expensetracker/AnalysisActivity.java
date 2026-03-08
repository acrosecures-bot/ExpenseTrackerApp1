package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;

public class AnalysisActivity extends AppCompatActivity {

    private Spinner spFilterCategory;
    private Button btnStartDate, btnEndDate, btnApply, btnShare;
    private TextView tvFilteredTotal;
    private RecyclerView rvFiltered;

    private ExpenseAdapter adapter;
    private List<Expense> filteredList;
    private List<Expense> allExpenses; // Local cache to make filtering fast

    private String startStr = "", endStr = "";
    private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // Bind Views
        spFilterCategory = findViewById(R.id.spFilterCategory);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnApply = findViewById(R.id.btnApplyFilter);
        btnShare = findViewById(R.id.btnShare);
        tvFilteredTotal = findViewById(R.id.tvFilteredTotal);

        // Setup RecyclerView
        rvFiltered = findViewById(R.id.rvFilteredExpenses);
        rvFiltered.setLayoutManager(new LinearLayoutManager(this));
        filteredList = new ArrayList<>();
        allExpenses = new ArrayList<>();
        adapter = new ExpenseAdapter(filteredList, null); // Pass null for delete if not needed
        rvFiltered.setAdapter(adapter);

        // Spinner Setup
        String[] categories = {"All", "Food", "Shopping", "Transport", "Rent", "Bills", "Other"};
        spFilterCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories));

        // Listeners
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnApply.setOnClickListener(v -> applyFilters());
        btnShare.setOnClickListener(v -> shareReport());

        loadInitialData();
    }

    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%02d-%02d-%d", day, month + 1, year);
            if (isStart) { startStr = date; btnStartDate.setText(date); }
            else { endStr = date; btnEndDate.setText(date); }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadInitialData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("Users").document(uid).collection("Expenses")
                .get().addOnSuccessListener(docs -> {
                    allExpenses.clear();
                    for (QueryDocumentSnapshot doc : docs) {
                        allExpenses.add(doc.toObject(Expense.class));
                    }
                    applyFilters(); // Initial view
                });
    }

    private void applyFilters() {
        String cat = spFilterCategory.getSelectedItem().toString();
        filteredList.clear();
        double total = 0;

        for (Expense exp : allExpenses) {
            boolean catMatch = cat.equals("All") || exp.category.equalsIgnoreCase(cat);
            boolean dateMatch = isWithinRange(exp.date);

            if (catMatch && dateMatch) {
                filteredList.add(exp);
                total += Double.parseDouble(exp.amount);
            }
        }


        tvFilteredTotal.setText("Filtered Total: ₹" + String.format("%.2f", total));
        adapter.notifyDataSetChanged();
    }

    private boolean isWithinRange(String expDateFull) {
        if (startStr.isEmpty() || endStr.isEmpty()) return true;
        try {
            Date current = sdf.parse(expDateFull.split(" ")[0]);
            Date start = sdf.parse(startStr);
            Date end = sdf.parse(endStr);
            return (current.equals(start) || current.after(start)) && (current.equals(end) || current.before(end));
        } catch (Exception e) { return true; }
    }

    private void shareReport() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No data to share", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder report = new StringBuilder("--- EXPENSE REPORT ---\n\n");
        double total = 0;

        for (Expense e : filteredList) {
            // Appending Title and Amount
            report.append("📌 ").append(e.title).append(": ₹").append(e.amount).append("\n");

            // Appending Date
            report.append("   Date: ").append(e.date).append("\n");

            // NEW: Appending Location if it exists
            if (e.location != null && !e.location.equals("Location not set")) {
                report.append("   📍 Location: ").append(e.location).append("\n");
            }

            report.append("----------------------\n");

            try {
                total += Double.parseDouble(e.amount);
            } catch (Exception ex) {
                // Handle parsing error if necessary
            }
        }

        report.append("\nTOTAL SUMMARY: ₹").append(String.format("%.2f", total));

        // Share Intent logic
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain"); // This allows sharing to WhatsApp, Telegram, and Email
        share.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(share, "Share Report via"));
    }}