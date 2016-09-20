package io.filepicker;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.UploadFileErrorEvent;
import io.filepicker.events.UploadProgressEvent;
import io.filepicker.models.FPFile;

/**
 * Created by maciejwitowski on 12/10/15.
 */
public class FilepickerCallbackHandler {

    Map<Uri, FilepickerCallback> callbacksMap = new HashMap<>();

    public void addCallback(Uri uri, FilepickerCallback filepickerCallback) {
        // First callback so the handler must be registered in EventBus
        if (callbacksMap.size() == 0) {
            register();
        }

        callbacksMap.put(uri, filepickerCallback);
    }

    @SuppressWarnings("unused")
    public void onEvent(FpFilesReceivedEvent event) {
        if (event == null) {
            return;
        }

        FPFile file = event.fpFiles.get(0);

        FilepickerCallback callback = callbacksMap.get(Uri.parse(file.getLocalPath()));
        if (callback != null) {
            callback.onFileUploadSuccess(file);
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(UploadFileErrorEvent event) {
        if (event == null) {
            return;
        }

        FilepickerCallback callback = callbacksMap.get(event.getUri());
        if (callback != null) {
            callback.onFileUploadError(new Exception(
                    "Uri: " + event.getUri() + ", Error: " + event.error
            ));
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(UploadProgressEvent event) {
        if (event == null) {
            return;
        }

        FilepickerCallback callback = callbacksMap.get(event.uri);
        if (callback != null) {
            callback.onFileUploadProgress(event.uri, event.progress);
        }
    }

    public void register() {
        EventBus.getDefault().register(this);
    }

    public void unregister() {
        callbacksMap.clear();
        EventBus.getDefault().unregister(this);
    }
}
