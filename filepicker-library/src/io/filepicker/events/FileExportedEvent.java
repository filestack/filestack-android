package io.filepicker.events;

import io.filepicker.models.FPFile;

/**
 * Created by maciejwitowski on 11/11/14.
 */
public final class FileExportedEvent {

    public final String path;
    public final FPFile fpFile;

    public FileExportedEvent(String path, FPFile fpFile) {
        this.path = path;
        this.fpFile = fpFile;
    }
}
