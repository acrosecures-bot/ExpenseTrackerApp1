package com.example.expensetracker;

public class Expense {
    public String id;
    public String title;
    public String category;
    public String amount;
    public String date; // HomeActivity will now use this name

    // Required for Firestore to convert database documents into Java objects
    public Expense() {}

    public Expense(String id, String title, String category, String amount, String date) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
    }
}