package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public ExpenseAdapter(List<Expense> expenseList, OnDeleteClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the layout for each row in the list
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense current = expenseList.get(position);

        // Sets the Title of the expense (e.g., Pizza)
        holder.tvTitle.setText(current.title);

        // Combines Category and Date for the detail line
        holder.tvCategory.setText(current.category + " • " + current.date);

        // Sets the amount with the Rupee symbol
        holder.tvAmount.setText("₹" + current.amount);

        // NEW: Check if location exists and update the view
        if (current.location != null && !current.location.isEmpty()) {
            holder.tvLocation.setText(current.location);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            // Hide the view if there is no location data for this entry
            holder.tvLocation.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position); // Triggers the delete callback
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size(); // Returns the total number of items
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvAmount, tvLocation; // Added tvLocation
        ImageButton btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            // Binding the XML views to Java objects
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvAmount = itemView.findViewById(R.id.tvItemAmount);
            tvLocation = itemView.findViewById(R.id.tvItemLocation); // Bind the new location TextView
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}