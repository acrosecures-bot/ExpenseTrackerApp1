package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnSubmit;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private boolean isSignUpMode = false; // Tracks which tab is active

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Splash screen delay logic
        new Handler().postDelayed(() -> {
            setTheme(R.style.Theme_ExpenseTracker);
            setContentView(R.layout.activity_login);
            initializeUI();
        }, 2500);
    }

    private void initializeUI() {
        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        tabLayout = findViewById(R.id.tabLayout);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        // Tab Switching Logic
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Login Mode
                    isSignUpMode = false;
                    etConfirmPassword.setVisibility(View.GONE);
                    btnSubmit.setText("LOGIN");
                } else {
                    // Sign Up Mode
                    isSignUpMode = true;
                    etConfirmPassword.setVisibility(View.VISIBLE);
                    btnSubmit.setText("SIGN UP");
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Main Button Logic
        btnSubmit.setOnClickListener(v -> handleAuth());
    }

    private void handleAuth() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. Basic Validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Sign Up Specific Validation
        if (isSignUpMode) {
            String confirmPass = etConfirmPassword.getText().toString().trim();
            if (!password.equals(confirmPass)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Minimum 6 characters required");
                return;
            }
        }

        // Show loading and disable button (Prevents the loop seen in your logs)
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        if (isSignUpMode) {
            performSignUp(email, password);
        } else {
            performLogin(email, password);
        }
    }

    private void performLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    resetUI();
                    if (task.isSuccessful()) {
                        showCustomToast("Login Successful!");
                        navigateToHome();
                    } else {
                        showError(task.getException());
                    }
                });
    }

    private void performSignUp(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    resetUI();
                    if (task.isSuccessful()) {
                        showCustomToast("Account Created!");
                        navigateToHome();
                    } else {
                        showError(task.getException());
                    }
                });
    }

    private void resetUI() {
        progressBar.setVisibility(View.INVISIBLE);
        btnSubmit.setEnabled(true);
    }

    private void showError(Exception e) {
        String message = (e != null) ? e.getMessage() : "Authentication Failed";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.tvToastMessage);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
}