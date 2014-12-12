package io.filepicker.models;

import io.filepicker.utils.Constants;

/**
 * Created by maciejwitowski on 10/22/14.
 */

/* NOTE: In this case, the word "Service" mean a phone service (like Camera or Gallery) or
   cloud service (like Dropbox or Facebook) and not a Service in Android sense
   (a component for running background operations) */

public final class Provider extends Node {

    public final String mimetypes;

    // Whether files can be saved to this provider or not
    public final boolean exportSupported;
    public final String code;

    public Provider(String displayName, String path, String mimetypes,
                   int drawable, boolean saveSupported, String code) {
        super(displayName, path, true, drawable);
        this.mimetypes = mimetypes;
        this.exportSupported = saveSupported;
        this.code = code;
    }

    public boolean handleAllMimetypes() {
        return this.mimetypes.equals(Constants.MIMETYPE_ALL);
    }
}
