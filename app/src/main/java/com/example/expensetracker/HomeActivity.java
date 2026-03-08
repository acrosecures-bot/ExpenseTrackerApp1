package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private FloatingActionButton fabAddExpense;
    private TextView tvTotalAmount;
    private PieChart pieChart;

    private RecyclerView rvExpenses;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final String TAG = "FIRESTORE_DEBUG";

    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private String currentAddress = "Location not set";

    private TextView tvUserName;
    private TextView tvUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // -------- NAVIGATION HEADER --------
        View headerView = navigationView.getHeaderView(0);

        tvUserName = headerView.findViewById(R.id.tvUserName);
        tvUserEmail = headerView.findViewById(R.id.tvUserEmail);

        if (mAuth.getCurrentUser() != null) {

            String uid = mAuth.getCurrentUser().getUid();
            tvUserEmail.setText(mAuth.getCurrentUser().getEmail());

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        if (documentSnapshot.exists()) {

                            String name = documentSnapshot.getString("name");

                            if (name != null) {
                                tvUserName.setText(name);
                            } else {
                                tvUserName.setText("User");
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to fetch user name"));
        }

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        pieChart = findViewById(R.id.pieChart);
        fabAddExpense = findViewById(R.id.fabAddExpense);

        rvExpenses = findViewById(R.id.rvExpenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));

        expenseList = new ArrayList<>();

        adapter = new ExpenseAdapter(expenseList,
                position -> deleteExpense(position));

        rvExpenses.setAdapter(adapter);

        fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        setupPieChart();
        loadData();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_calculator) {
            startActivity(new Intent(this, CalculatorActivity.class));

        } else if (id == R.id.nav_currency) {
            startActivity(new Intent(this, CurrencyActivity.class));

        } else if (id == R.id.nav_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));

        } else if (id == R.id.nav_logout) {

            SharedPreferences sharedPreferences =
                    getSharedPreferences("UserSession", MODE_PRIVATE);

            SharedPreferences.Editor editor =
                    sharedPreferences.edit();

            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            mAuth.signOut();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ---------------- ADD EXPENSE DIALOG ----------------

    private void showAddExpenseDialog() {

        fusedLocationClient =
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_expense, null);

        builder.setView(view);

        EditText etTitle = view.findViewById(R.id.etExpenseTitle);
        EditText etAmount = view.findViewById(R.id.etExpenseAmount);

        Spinner spCategory = view.findViewById(R.id.spCategory);

        Button btnDatePicker = view.findViewById(R.id.btnDatePicker);
        Button btnTimePicker = view.findViewById(R.id.btnTimePicker);
        Button btnGetLocation = view.findViewById(R.id.btnGetLocation);
        Button btnSave = view.findViewById(R.id.btnAddExpenseConfirm);

        String[] categories =
                {"Food","Shopping","Transport","Rent","Bills","Other"};

        spCategory.setAdapter(
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        categories));

        final Calendar calendar = Calendar.getInstance();

        currentAddress = "Location not set";

        btnDatePicker.setOnClickListener(v -> {

            new DatePickerDialog(this,
                    (view1, year, month, dayOfMonth) -> {

                        calendar.set(Calendar.YEAR,year);
                        calendar.set(Calendar.MONTH,month);
                        calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);

                        String selectedDate =
                                new SimpleDateFormat("dd-MM-yyyy",
                                        Locale.getDefault())
                                        .format(calendar.getTime());

                        btnDatePicker.setText(selectedDate);

                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTimePicker.setOnClickListener(v -> {

            new TimePickerDialog(this,
                    (view1, hourOfDay, minute) -> {

                        calendar.set(Calendar.HOUR_OF_DAY,hourOfDay);
                        calendar.set(Calendar.MINUTE,minute);

                        String selectedTime =
                                new SimpleDateFormat("HH:mm",
                                        Locale.getDefault())
                                        .format(calendar.getTime());

                        btnTimePicker.setText(selectedTime);

                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true).show();
        });

        btnSave.setOnClickListener(v -> {

            String title = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = spCategory.getSelectedItem().toString();

            String formattedDate =
                    new SimpleDateFormat("dd-MM-yyyy HH:mm",
                            Locale.getDefault())
                            .format(calendar.getTime());

            if(title.isEmpty() || amountStr.isEmpty()){
                Toast.makeText(this,"Fill all fields",Toast.LENGTH_SHORT).show();
                return;
            }

            String userId =
                    mAuth.getCurrentUser()!=null ?
                            mAuth.getCurrentUser().getUid() : null;

            if(userId==null) return;

            DocumentReference newDoc =
                    db.collection("Users")
                            .document(userId)
                            .collection("Expenses")
                            .document();

            Expense expense =
                    new Expense(newDoc.getId(),
                            title,category,
                            amountStr,formattedDate,currentAddress);

            newDoc.set(expense)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show());
        });

        builder.create().show();
    }

    // ---------------- PIE CHART ----------------

    private void setupPieChart(){

        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.animateY(1400);
    }

    private void loadData(){

        if(mAuth.getCurrentUser()==null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("Users")
                .document(userId)
                .collection("Expenses")
                .addSnapshotListener((value,error)->{

                    if(value!=null){

                        expenseList.clear();

                        double total=0;
                        Map<String,Float> categoryMap=new HashMap<>();

                        for(QueryDocumentSnapshot doc:value){

                            Expense expense =
                                    doc.toObject(Expense.class);

                            expenseList.add(expense);

                            try{

                                float amt =
                                        Float.parseFloat(expense.amount);

                                total += amt;

                                categoryMap.put(
                                        expense.category,
                                        categoryMap.getOrDefault(
                                                expense.category,0f)+amt);

                            }catch(Exception ignored){}
                        }

                        adapter.notifyDataSetChanged();

                        tvTotalAmount.setText(
                                "₹"+String.format("%.2f",total));

                        updateChart(categoryMap);
                    }
                });
    }

    private void updateChart(Map<String,Float> categoryMap){

        if(categoryMap.isEmpty()){
            pieChart.clear();
            return;
        }

        List<PieEntry> entries=new ArrayList<>();

        for(Map.Entry<String,Float> entry:categoryMap.entrySet()){
            entries.add(new PieEntry(entry.getValue(),entry.getKey()));
        }

        PieDataSet dataSet=new PieDataSet(entries,"");

        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data=new PieData(dataSet);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void deleteExpense(int position){

        if(position>=expenseList.size()) return;

        String id = expenseList.get(position).id;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("Users")
                .document(userId)
                .collection("Expenses")
                .document(id)
                .delete();
    }
}