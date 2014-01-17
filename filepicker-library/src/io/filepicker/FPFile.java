package io.filepicker;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

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
	public FPFile(String[] archive, JSONObject data) {
		this.localpath = archive[0];
		
		try {
			this.fpurl = data.getString("url");
			JSONObject fileData = data.getJSONObject("data");
			this.size = fileData.getLong("size");
			this.type = fileData.getString("type");
			this.key = "AE7oXsxqxQTi5dAOrOwiZz";//fileData.getString("key"); TODO PATCH!!
			this.filename = archive[1];//fileData.getString("filename");  TODO PATCH!!
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	//TODO PATCH!! (May deprecated by the next method)
	public FPFile(String localpath, JSONObject data) {
		this.localpath = localpath;
		try {
			this.fpurl = data.getString("url");
			JSONObject fileData = data.getJSONObject("data");
			this.size = fileData.getLong("size");
			this.type = fileData.getString("type");
			this.key = FilePickerAPI.FPAPIKEY;//fileData.getString("key"); TODO PATCH!!
			this.filename = fileData.getString("filename");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	//TODO Suggested solution
	public FPFile(String string, String pathFilename, JSONObject data) {
		this.localpath = string;
		
		try {
			this.fpurl = data.getString("url");
			JSONObject fileData = data.getJSONObject("data");
			this.size = fileData.getLong("size");
			this.type = fileData.getString("type");
			this.key = FilePickerAPI.FPAPIKEY;//fileData.getString("key"); TODO PATCH!!
			this.filename = pathFilename;//fileData.getString("filename");  TODO PATCH!!
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public String getLocalPath() {
		return this.localpath;
	}

	public String getFPUrl() {
		return this.fpurl;
	}

	public long getSize() {
		return this.size;
	}

	public String getType() {
		return this.type;
	}

	public String getKey() {
		return this.key;
	}
	
	public String getFilename() {
		return this.filename;
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
				+ ", filename: " + filename
				+ ", type: " + type;
	}
}
