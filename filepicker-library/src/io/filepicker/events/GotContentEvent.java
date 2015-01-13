package io.filepicker.events;

import io.filepicker.models.Folder;

/**
 * Created by maciejwitowski on 10/24/14.
 */
public final class GotContentEvent {

    public final Folder folder;
    public final boolean backPressed;

    public GotContentEvent(Folder folder, boolean backPressed) {
        this.folder = folder;
        this.backPressed = backPressed;
    }
}
