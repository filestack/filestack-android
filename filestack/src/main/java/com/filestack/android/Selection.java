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

    /** Constructor for local file.
     *
     * @deprecated Creation of this class should not be available to any consumers of the library.
     *     Access to this class is scheduled to be removed in future versions.
     * */
    @Deprecated
    public Selection(String provider, String path, String mimeType, String name) {
        this.provider = provider;
        this.path = path;
        this.mimeType = mimeType;
        this.name = name;
    }

    /** Constructor for local file.
     *
     * @deprecated Creation of this class should not be available to any consumers of the library.
     *     Access to this class is scheduled to be removed in future versions.
     * */
    @Deprecated
    public Selection(String provider, Uri uri, int size, String mimeType, String name) {
        this.provider = provider;
        this.uri = uri;
        this.size = size;
        this.mimeType = mimeType;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Selection selection = (Selection) o;

        if (size != selection.size) return false;
        if (provider != null ? !provider.equals(selection.provider) : selection.provider != null)
            return false;
        if (path != null ? !path.equals(selection.path) : selection.path != null) return false;
        if (uri != null ? !uri.equals(selection.uri) : selection.uri != null) return false;
        if (mimeType != null ? !mimeType.equals(selection.mimeType) : selection.mimeType != null)
            return false;
        return name != null ? name.equals(selection.name) : selection.name == null;
    }

    @Override
    public int hashCode() {
        int result = provider != null ? provider.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + size;
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
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
