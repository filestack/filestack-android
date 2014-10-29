package io.filepicker.events;

import java.util.ArrayList;

import io.filepicker.models.FPFile;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public class FpFilesReceivedEvent {

    ArrayList<FPFile> fpFiles;

    public FpFilesReceivedEvent(ArrayList<FPFile> fpFiles) {
        this.fpFiles = fpFiles;
    }

    public ArrayList<FPFile> getFpFiles() {
        return fpFiles;
    }
}
