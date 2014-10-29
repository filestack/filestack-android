package io.filepicker.events;

import io.filepicker.models.Folder;

/**
 * Created by maciejwitowski on 10/24/14.
 */
public class GetContentEvent {

    private Folder folder;

    public GetContentEvent(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }
}
