package io.filepicker.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public class FPFile implements Parcelable {

    String container;
    String url;
    String filename;
    String key;
    String type;
    long size;

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
        this.container = in.readString();
        this.url = in.readString();
        this.filename = in.readString();
        this.key = in.readString();
        this.type = in.readString();
        this.size = in.readLong();
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
    }
}
