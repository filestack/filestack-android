package io.filepicker.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by maciejwitowski on 10/22/14.
 */

/* NOTE: In this case, the word "Service" mean a phone service (like Camera or Gallery) or
   cloud service (like Dropbox or Facebook) and not a Service in Android sense
   (a component for running background operations) */

public class Provider extends Node {

    String mimetypes;
    boolean saveSupported;
    String code;

    public Provider(String displayName, String path, String mimetypes,
                   int drawable, boolean saveSupported, String code) {
        super(displayName, path, true, drawable);
        this.mimetypes = mimetypes;
        this.saveSupported = saveSupported;
        this.code = code;
    }

    public String getMimetypes() {
        return mimetypes;
    }

    public String getCode() {
        return code;
    }
}
