package com.example.expensetracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSubmit;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isSignUpMode = false;

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences =
                getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        boolean isLoggedIn =
                sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn && mAuth.getCurrentUser() != null) {
            navigateToHome();
            return;
        }

        new Handler().postDelayed(() -> {
            setTheme(R.style.Theme_ExpenseTracker);
            setContentView(R.layout.activity_login);
            initializeUI();
        }, 2000);
    }

    private void initializeUI() {

        tabLayout = findViewById(R.id.tabLayout);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                if (tab.getPosition() == 0) {

                    isSignUpMode = false;

                    etName.setVisibility(View.GONE);
                    etConfirmPassword.setVisibility(View.GONE);

                    btnSubmit.setText("LOGIN");

                } else {

                    isSignUpMode = true;

                    etName.setVisibility(View.VISIBLE);
                    etConfirmPassword.setVisibility(View.VISIBLE);

                    btnSubmit.setText("SIGN UP");
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}

            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnSubmit.setOnClickListener(v -> handleAuth());
    }

    private void handleAuth() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSignUpMode) {

            if (name.isEmpty()) {
                etName.setError("Enter your name");
                return;
            }

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

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        if (isSignUpMode) {
            performSignUp(name, email, password);
        } else {
            performLogin(email, password);
        }
    }

    private void performLogin(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    resetUI();

                    if (task.isSuccessful()) {

                        saveLoginStatus(true);

                        showCustomToast("Login Successful!");

                        navigateToHome();

                    } else {

                        showError(task.getException());
                    }
                });
    }

    private void performSignUp(String name, String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    resetUI();

                    if (task.isSuccessful()) {

                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);

                        db.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> {

                                    saveLoginStatus(true);

                                    showCustomToast("Account Created!");

                                    navigateToHome();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Failed to store user data",
                                                Toast.LENGTH_LONG).show());

                    } else {

                        showError(task.getException());
                    }
                });
    }

    private void saveLoginStatus(boolean status) {

        SharedPreferences sharedPreferences =
                getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean(KEY_IS_LOGGED_IN, status);

        editor.apply();
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