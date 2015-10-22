package io.filepicker.events;

import android.net.Uri;

public final class UploadProgressEvent {

    public final Uri uri;
    public final float progress;

    public UploadProgressEvent(Uri uri, float progress) {
        this.uri = uri;
        this.progress = progress;
    }
}
