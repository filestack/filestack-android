package io.filepicker;

import java.util.concurrent.RejectedExecutionException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;


class ThumbnailView extends android.widget.ImageView {
	private final String url;
	private final Inode inode;
	private ThumbnailLoaderTask task = null;
	private int width;

	class ThumbnailLoaderTask extends
			AsyncTask<Void, Integer, Bitmap> {

		@Override
		protected Bitmap doInBackground(Void... arg0) {
			if (inode.getThumbnailBitmap() != null)
				return inode.getThumbnailBitmap();
			Bitmap bitmap = FilePickerAPI.getInstance().getThumbnail(url);
			int MAXSIZE = width;
			if (bitmap.getHeight() > MAXSIZE || bitmap.getWidth() > MAXSIZE) {
				//scale
				int h = bitmap.getHeight();
				int w = bitmap.getWidth();
				h = h * Math.min(w, MAXSIZE) / w;
				w = Math.min(w, MAXSIZE);
				w = w * Math.min(h, MAXSIZE) / h;
				h = Math.min(h, MAXSIZE);
				return Bitmap.createScaledBitmap(bitmap, w, h, true);
			} else {
				return bitmap;
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				ThumbnailView.this.setImageBitmap(result);
				ThumbnailView.this.requestLayout();
				if (inode.getThumbnailBitmap() == null)
					inode.setThumbnailBitmap(result);
			}
			task = null;
		}

	}
	
	@SuppressLint("NewApi")
	public ThumbnailView(Context context, Inode inode) {
		super(context);
		setBackgroundColor(Color.GRAY);
		width = context.getResources().getDisplayMetrics().widthPixels / 3;
		setMinimumHeight(width);
		setMinimumWidth(width);
		setScaleType(ScaleType.CENTER_CROP);
		this.url = inode.getThumbnail();
		this.inode = inode;
		if (inode.getThumbnailBitmap() != null) {
			setImageBitmap(inode.getThumbnailBitmap());
			requestLayout();
		} else {
			int SDK_INT = android.os.Build.VERSION.SDK_INT;
			task = new ThumbnailLoaderTask();
			if (SDK_INT >= 12) {
				try {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} catch (RejectedExecutionException e) {
					//task.execute();
				}
			} else {
				task.execute();
			}
		}
	}
	
	@Override
	public void onDetachedFromWindow() {
		if (task != null)
			task.cancel(true);
	}

}
