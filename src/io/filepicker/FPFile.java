package io.filepicker;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class FPFile implements Parcelable {
	
	private final String localpath;
	private final String fpurl;
	private final long size;
	private final String type;
	private final String key;
	private final String filename;

	/**
	 * Parcelable factory
	 */
	public static final Parcelable.Creator<FPFile> CREATOR =  new Parcelable.Creator<FPFile>() {
		public FPFile createFromParcel(Parcel in) {
			return new FPFile(in);
		}

		public FPFile[] newArray(int size) {
			return new FPFile[size];
		}
	};

	/**
	 * Parcelable constructor
	 * @param in
	 */
	public FPFile(Parcel in) {
		//The order of these variables must match exactly to the order
		//in the parcel writer
		this.localpath = in.readString();
		this.fpurl = in.readString();
		this.size = in.readLong();
		this.type = in.readString();
		this.key = in.readString();
		this.filename = in.readString();
	}

	/**
	 * Explicit constructor
	 * 
	 * @param localpath
	 * @param fpurl
	 * @param size
	 * @param type
	 * @param key
	 * @param filename
	 */
	public FPFile(String localpath, String fpurl, long size, String type, String key, String filename) {
		this.localpath = localpath;
		this.fpurl = fpurl;
		this.size = size;
		this.type = type;
		this.key = key;
		this.filename = filename;
	}

	/**
	 * Construct FPFile based on response. Must be of the format
	 * <pre>
	 * {@code
	 * {
     *   "url": "https://www.filepicker.io/api/file/CAoBl1bORiOXQVZMUyXM",
     *   "data": {
     *   "size": 2287265,
     *     "type": "text/plain",
     *     "key": "498rgTBaQW6rub4rRftq_testfile.file",
     *     "filename": "testfile.file"
     *   }
     * }
	 * </pre>
	 * @param localpath
	 * @param data
	 */
	public FPFile(String localpath, JSONObject data) {
		this.localpath = localpath;
		try {
			this.fpurl = data.getString("url");
			JSONObject fileData = data.getJSONObject("data");
			this.size = fileData.getLong("size");
			this.type = fileData.getString("type");
			this.key = fileData.getString("key");
			this.filename = fileData.getString("filename");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public String getLocalPath() {
		return localpath;
	}

	public String getFPUrl() {
		return fpurl;
	}

	public long getSize() {
		return size;
	}

	public String getType() {
		return type;
	}

	public String getKey() {
		return key;
	}
	
	public String getFilename() {
		return filename;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		//The order of these variables must match exactly to the order
		//in the parcel constructor
		out.writeString(localpath);
		out.writeString(fpurl);
		out.writeLong(size);
		out.writeString(type);
		out.writeString(key);
		out.writeString(filename);
	}
	
	@Override
	public String toString() {
		return FPFile.class.getSimpleName() 
				+ ", filename " + filename
				+ ", type " + type;
	}
}
