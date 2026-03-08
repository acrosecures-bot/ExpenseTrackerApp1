package com.example.expensetracker;

public class Expense {
    public String id, title, category, amount, date, location; // Added location

    public Expense() {}

    public Expense(String id, String title, String category, String amount, String date, String location) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.location = location; //
    }
}
