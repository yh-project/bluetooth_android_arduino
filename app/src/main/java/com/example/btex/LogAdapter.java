package com.example.btex;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
    Context context;
    ArrayList<LogData> items;

    public LogAdapter(ArrayList<LogData> items) {
        this.items = items;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = null;
        if(viewType == 1)
            itemView = inflater.inflate(R.layout.item_from, parent, false);
        else if(viewType == 0)
            itemView = inflater.inflate(R.layout.item_to, parent, false);
        else if(viewType == 2)
            itemView = inflater.inflate(R.layout.item_start, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogData item = items.get(position);
        holder.setItem(item);
    }

    public void addItem(LogData item) {
        items.add(item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date;
        TextView content;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            date = itemView.findViewById(R.id.date);
            content = itemView.findViewById(R.id.content);
        }

        public void setItem(LogData item) {
            if(item.date == null)
                Log.d("키읔", "null이에연");
            else
                date.setText(item.date);
            content.setText(item.content);
        }
    }
}
