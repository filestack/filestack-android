package io.filepicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

class NonThumbnailGridBlockView extends android.widget.LinearLayout {
	private int width;

	public NonThumbnailGridBlockView(Context context, Inode inode) {
		super(context);
		width = context.getResources().getDisplayMetrics().widthPixels / 3;
		setMinimumHeight(width);
		setMinimumWidth(width);
		setGravity(Gravity.CENTER);
		
		setOrientation(LinearLayout.VERTICAL);
		TextView textView = new TextView(context);
		textView.setTextSize(14.0f);
		
		textView.setText(inode.getDisplayName());
		textView.setGravity(Gravity.CENTER);
		
		ImageView icon = new ImageView(context);
		icon.setImageResource(inode.getImageResource());
		if (inode.isDisabled()) {
			textView.setTextColor(Color.GRAY);
			icon.setColorFilter(0xff888888, Mode.SRC_ATOP); //argb
		} else {
			textView.setTextColor(Color.WHITE);
			icon.setColorFilter(0xffffffff, Mode.SRC_ATOP); //argb
		}
		setBackgroundColor(Color.BLACK);
		
		addView(icon);
		addView(textView);
	}

}
