package com.filestack.android.internal;

import android.content.Context;
import android.content.res.ColorStateList;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.filestack.android.R;
import com.filestack.android.Theme;
import com.squareup.picasso.Picasso;

/**
 * Holds references to view elements inside {{@link RecyclerView}} list items.
 * {{@link CloudListFragment}}, {{@link CloudListAdapter}}, and {{@link CloudListViewHolder}} work
 * together to create the cloud sources list.
 *
 * @see <a href="https://developer.android.com/guide/topics/ui/layout/recyclerview">
 *     https://developer.android.com/guide/topics/ui/layout/recyclerview</a>
 */
class CloudListViewHolder extends RecyclerView.ViewHolder {
    private TextView nameView;
    private TextView infoView;
    private ImageView iconView;
    private ImageView checkboxView;

    CloudListViewHolder(View listItemView) {
        super(listItemView);
        this.nameView = listItemView.findViewById(R.id.name);
        this.infoView = listItemView.findViewById(R.id.info);
        this.iconView = listItemView.findViewById(R.id.icon);
        this.checkboxView = listItemView.findViewById(R.id.checkbox);
    }

    public void setId(int id) {
        itemView.setId(id);
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

    public void setInfoVisible(boolean isVisible) {
        if (infoView != null) {
            infoView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    public void setIcon(String url) {
        if (iconView != null) {
            Context context = iconView.getContext();
            Picasso.with(context).load(url).into(iconView);
        }
    }

    public void apply(Theme theme) {
        if (nameView != null) {
            nameView.setTextColor(theme.getTextColor());
        }

        if (infoView!= null) {
            infoView.setTextColor(ColorUtils.setAlphaComponent(theme.getTextColor(), 220));
        }

        ImageViewCompat.setImageTintList(checkboxView, ColorStateList.valueOf(theme.getAccentColor()));

    }

    void setOnClickListener(View.OnClickListener listener) {
        itemView.setOnClickListener(listener);
    }

    void setSelected(boolean selected) {
        if (selected) {
            checkboxView.setVisibility(View.VISIBLE);
        } else {
            checkboxView.setVisibility(View.INVISIBLE);
        }
    }

    void setEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.45f;
        iconView.setAlpha(alpha);
        if (nameView != null) {
            nameView.setAlpha(alpha);
            infoView.setAlpha(alpha);
        }
    }
}
