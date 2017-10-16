package com.filestack.android;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

class CloudListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private int id;

    private TextView nameView;
    private TextView infoView;
    private ImageView iconView;

    CloudListItemViewHolder(View listItemView) {
        super(listItemView);
        this.nameView = listItemView.findViewById(R.id.name);
        this.infoView = listItemView.findViewById(R.id.info);
        this.iconView = listItemView.findViewById(R.id.icon);
        listItemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

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
}
