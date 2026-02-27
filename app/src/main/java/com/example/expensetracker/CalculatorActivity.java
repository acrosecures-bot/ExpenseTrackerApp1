package com.example.expensetracker;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CalculatorActivity extends AppCompatActivity {

    private TextView tvDisplay;
    private String currentInput = "";
    private String lastOperator = "";
    private double firstValue = Double.NaN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        tvDisplay = findViewById(R.id.tvDisplay);

        // Click listeners for numbers
        int[] numIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot};
        View.OnClickListener numListener = v -> {
            Button b = (Button) v;
            currentInput += b.getText().toString();
            tvDisplay.setText(currentInput);
        };
        for (int id : numIds) findViewById(id).setOnClickListener(numListener);

        // Operator listeners
        findViewById(R.id.btnAdd).setOnClickListener(v -> setOp("+"));
        findViewById(R.id.btnSub).setOnClickListener(v -> setOp("-"));
        findViewById(R.id.btnMult).setOnClickListener(v -> setOp("×"));
        findViewById(R.id.btnDiv).setOnClickListener(v -> setOp("÷"));

        findViewById(R.id.btnAC).setOnClickListener(v -> {
            currentInput = ""; firstValue = Double.NaN; tvDisplay.setText("0");
        });

        findViewById(R.id.btnEqual).setOnClickListener(v -> calculate());
    }

    private void setOp(String op) {
        if (!currentInput.isEmpty()) {
            firstValue = Double.parseDouble(currentInput);
            lastOperator = op;
            currentInput = "";
        }
    }

    private void calculate() {
        if (!Double.isNaN(firstValue) && !currentInput.isEmpty()) {
            double secondValue = Double.parseDouble(currentInput);
            double result = 0;
            switch (lastOperator) {
                case "+": result = firstValue + secondValue; break;
                case "-": result = firstValue - secondValue; break;
                case "×": result = firstValue * secondValue; break;
                case "÷": result = firstValue / secondValue; break;
            }
            tvDisplay.setText(String.valueOf(result));
            currentInput = String.valueOf(result);
            firstValue = Double.NaN;
        }
    }
}