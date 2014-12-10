package io.filepicker.events;

import java.util.ArrayList;

import io.filepicker.models.FPFile;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public final class FpFilesReceivedEvent {

    public final ArrayList<FPFile> fpFiles;

    public FpFilesReceivedEvent(ArrayList<FPFile> fpFiles) {
        this.fpFiles = fpFiles;
    }
}
