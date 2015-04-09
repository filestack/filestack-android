package io.filepicker.events;

import android.net.Uri;

/**
 * Created by maciejwitowski on 4/8/15.
 */
public class UploadFileErrorEvent extends ApiErrorEvent {

    private Uri uri;

    public UploadFileErrorEvent(Uri fileUri, ErrorType error) {
        super(error);
        this.uri = fileUri;
    }

    public Uri getUri() {
        return uri;
    }
}