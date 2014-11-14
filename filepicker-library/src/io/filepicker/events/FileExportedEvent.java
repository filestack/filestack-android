package io.filepicker.events;

import io.filepicker.models.FPFile;

/**
 * Created by maciejwitowski on 11/11/14.
 */
public class FileExportedEvent {

    private final String path;
    private final FPFile fpFile;

    public FileExportedEvent(String path, FPFile fpFile) {
        this.path = path;
        this.fpFile = fpFile;
    }

    public String getPath() {
        return path;
    }

    public FPFile getFpFile() {
        return fpFile;
    }
}
