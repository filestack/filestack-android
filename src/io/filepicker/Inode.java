package io.filepicker;

import io.filepicker.R;

import android.graphics.Bitmap;

class Inode {
	private String displayName;
	private String path;
	private boolean is_dir;
	private boolean thumb_exists = false;
	private Bitmap thumbnailBitmap = null;
	private String thumbnail = null;
	private int imageResource;
	private boolean disabled = false;

	
	public boolean getThumb_exists() {
		return thumb_exists;
	}

	public void setThumb_exists(boolean thumb_exists) {
		this.thumb_exists = thumb_exists;
	}

	public String getThumbnail() {
		return thumbnail;
	}
	
	public Bitmap getThumbnailBitmap() {
		return thumbnailBitmap;
	}
	
	public void setThumbnailBitmap(Bitmap b) {
		this.thumbnailBitmap = b;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public Inode(String displayName, String path, boolean is_dir) {
		this.displayName = displayName;
		this.path = path;
		this.is_dir = is_dir;
		this.imageResource = -1;
	}
	
	public Inode(String displayName, String path, boolean is_dir, int imageResource) {
		this.displayName = displayName;
		this.path = path;
		this.is_dir = is_dir;
		this.imageResource = imageResource;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getPath() {
		return this.path;
	}
	
	public boolean getIsDir() {
		return this.is_dir;
	}
	
	public int getImageResource() {
		if (imageResource != -1) {
			return imageResource;
		} else {
			if (is_dir) {
				return R.drawable.glyphicons_144_folder_open; 
			} else {
				return R.drawable.glyphicons_036_file;
			}
		}
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public boolean isDisabled() {
		return this.disabled;
	}
}