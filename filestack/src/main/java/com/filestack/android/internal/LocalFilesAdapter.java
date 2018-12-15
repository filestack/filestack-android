package com.filestack.android.internal;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.filestack.android.R;

import java.util.ArrayList;

public class LocalFilesAdapter extends RecyclerView.Adapter<LocalFilesAdapter.CustomViewHolder> {
    private ArrayList<String> fileNames = new ArrayList<>();

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.filestack__local_files_list_item, parent, false);
        return new CustomViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        holder.text.setText(fileNames.get(position));
    }

    @Override
    public int getItemCount() {
        return fileNames.size();
    }

    void updateFileNames(ArrayList<String> fileNames) {
        this.fileNames = fileNames;
        notifyDataSetChanged();
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        protected TextView text;

        CustomViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text_id);
        }
    }
}
