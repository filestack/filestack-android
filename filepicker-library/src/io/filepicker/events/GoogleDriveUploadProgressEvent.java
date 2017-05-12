package io.filepicker.events;

import io.filepicker.models.GoogleDriveNode;

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
