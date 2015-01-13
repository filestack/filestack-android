package io.filepicker.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public final class FPFile implements Parcelable {

    private String container;
    private String url;
    private String filename;
    private String key;
    private String type;
    private String localPath;
    private long size;

    public FPFile(String container, String url, String filename, String key, String type, long size) {
        this.container = container;
        this.url = url;
        this.filename = filename;
        this.key = key;
        this.type = type;
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public String getKey() {
        return key;
    }

    public long getSize() {
        return size;
    }

    public String getContainer() {
        return container;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getLocalPath() {
        return localPath;
    }

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

    // Takes content uri like "content://..." and returns file name
    public static String contentUriToFilename(Uri contentUri) {
        String filename = new File(contentUri.getPath()).getName();
        return filename.substring(filename.indexOf("_") + 1);
    }
    /**
     * Parcelable constructor
     * @param in
     */
    public FPFile(Parcel in) {
        //The order of these variables must match exactly to the order
        //in the parcel writer
        this.container = in.readString();
        this.url = in.readString();
        this.filename = in.readString();
        this.key = in.readString();
        this.type = in.readString();
        this.size = in.readLong();
        this.localPath = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //The order of these variables must match exactly to the order
        //in the parcel constructor
        out.writeString(container);
        out.writeString(url);
        out.writeString(filename);
        out.writeString(key);
        out.writeString(type);
        out.writeLong(size);
        out.writeString(localPath);
    }
}
