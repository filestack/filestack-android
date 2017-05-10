package io.filepicker.events;

import io.filepicker.models.GoogleDriveNode;

/**
 * Created by Raúl Acevedo - SWEB
 * on 25/04/2017.
 */

public class GoogleDriveUploadProgressEvent {

    private GoogleDriveNode node;
    private float progress;

    public GoogleDriveUploadProgressEvent(GoogleDriveNode node, float progress) {
        this.node = node;
        this.progress = progress;
    }

    public GoogleDriveNode getNode() {
        return node;
    }

    public float getProgress() {
        return progress;
    }
}
