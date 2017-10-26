package com.filestack.android;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

class CloudListViewHolder extends RecyclerView.ViewHolder {
    private int id;

    private View itemView;
    private TextView nameView;
    private TextView infoView;
    private ImageView iconView;
    private ImageView checkboxView;

    CloudListViewHolder(View listItemView) {
        super(listItemView);
        this.itemView = listItemView;
        this.nameView = listItemView.findViewById(R.id.name);
        this.infoView = listItemView.findViewById(R.id.info);
        this.iconView = listItemView.findViewById(R.id.icon);
        this.checkboxView = listItemView.findViewById(R.id.checkbox);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        if (nameView != null) {
            nameView.setText(name);
        }
    }

    public void setInfo(String info) {
        if (infoView != null) {
            infoView.setText(info);
        }
    }

    public void setIcon(String url) {
        if (iconView != null) {
            Context context = iconView.getContext();
            Picasso.with(context).load(url).into(iconView);
        }
    }

    void setOnClickListener(View.OnClickListener listener) {
        itemView.setOnClickListener(listener);
    }

    void setOnLongClickListener(View.OnLongClickListener listener) {
        itemView.setOnLongClickListener(listener);
    }

    void setSelected(boolean selected) {
        if (selected) {
            checkboxView.setVisibility(View.VISIBLE);
        } else {
            checkboxView.setVisibility(View.INVISIBLE);
        }
    }
}
