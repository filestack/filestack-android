package io.filepicker;

import android.net.Uri;

import io.filepicker.models.FPFile;

/**
 * Created by maciejwitowski on 12/10/15.
 */
public interface FilepickerCallback {

    void onFileUploadSuccess(FPFile fpFile);

    void onFileUploadError(Throwable error);

    void onFileUploadProgress(Uri uri, float progress);

}
