package com.example.expensetracker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class CurrencyActivity extends AppCompatActivity {

    private EditText etAmount;
    private RadioButton rbInrToUsd;
    private Button btnConvert;
    private TextView tvResult;

    // Fixed Exchange Rate for 2026
    private final double RATE = 83.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);

        etAmount = findViewById(R.id.etAmountToConvert);
        rbInrToUsd = findViewById(R.id.rbInrToUsd);
        btnConvert = findViewById(R.id.btnConvert);
        tvResult = findViewById(R.id.tvResult);

        btnConvert.setOnClickListener(v -> {
            String input = etAmount.getText().toString();
            if (input.isEmpty()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(input);
            double result;

            if (rbInrToUsd.isChecked()) {
                // INR to USD
                result = amount / RATE;
                tvResult.setText(String.format("Result: $ %.2f", result));
            } else {
                // USD to INR
                result = amount * RATE;
                tvResult.setText(String.format("Result: ₹ %.2f", result));
            }
        });
    }
}