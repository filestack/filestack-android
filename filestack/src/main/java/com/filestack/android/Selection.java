package com.filestack.android;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/** Metadata for a user selection (but not yet upload) of a local or cloud file. */
public class Selection implements Parcelable {
    public static final Parcelable.Creator<Selection> CREATOR = new Creator();

    private String provider;
    private String path;
    private Uri uri;
    private int size;
    private String mimeType;
    private String name;

    /** Constructor for cloud file. */
    public Selection(String provider, String path, String mimeType, String name) {
        this.provider = provider;
        this.path = path;
        this.mimeType = mimeType;
        this.name = name;
    }

    /** Constructor for local file. */
    public Selection(String provider, Uri uri, int size, String mimeType, String name) {
        this.provider = provider;
        this.uri = uri;
        this.size = size;
        this.mimeType = mimeType;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Selection)) {
            return false;
        }

        Selection other = (Selection) obj;

        if (!this.getProvider().equals(other.getProvider())) {
            return false;
        }

        if (this.getPath() != null) {
            if (other.getPath() == null) {
                return false;
            }
            if (!this.getPath().equals(other.getPath())) {
                return false;
            }
        }

        if (this.getUri() != null) {
            if (other.getUri() == null) {
                return false;
            }
            if (!this.getUri().equals(other.getUri())) {
                return false;
            }
            if (this.getSize() != other.getSize()) {
                return false;
            }
        }

        return this.getName().equals(other.getName());
    }

    /** The source this file came from. */
    public String getProvider() {
        return provider;
    }

    /** The path of this file in a cloud provider. Null if a local file. */
    public String getPath() {
        return path;
    }

    /** The Android system URI of this file. Null if a cloud file. */
    public Uri getUri() {
        return uri;
    }

    /** Size in bytes. */
    public int getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getName() {
        return name;
    }

    // For Parcelable interface

    private static class Creator implements Parcelable.Creator<Selection> {
        @Override
        public Selection createFromParcel(Parcel in) {
            return new Selection(in);
        }

        @Override
        public Selection[] newArray(int size) {
            return new Selection[size];
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(provider);
        out.writeString(path);
        out.writeParcelable(uri, flags);
        out.writeInt(size);
        out.writeString(mimeType);
        out.writeString(name);
    }

    private Selection(Parcel in) {
        provider = in.readString();
        path = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
        size = in.readInt();
        mimeType = in.readString();
        name = in.readString();
    }
}
