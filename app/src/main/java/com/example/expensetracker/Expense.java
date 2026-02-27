package com.example.expensetracker;

public class Expense {
    public String id;
    public String title;    // Added this to match your XML
    public String category;
    public String amount;
    public String date;

    public Expense() {} // Required for Firebase

    public Expense(String id, String title, String category, String amount, String date) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
    }
}