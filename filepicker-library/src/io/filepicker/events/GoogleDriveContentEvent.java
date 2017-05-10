package io.filepicker.events;

import java.util.ArrayList;

import io.filepicker.models.GoogleDriveNode;

/**
 * Created by Raúl Acevedo - SWEB
 * on 21/04/2017.
 */

public class GoogleDriveContentEvent {

    private ArrayList<GoogleDriveNode> googleNodes;
    private boolean backPresed;
    private boolean loadMore;

    public GoogleDriveContentEvent(ArrayList<GoogleDriveNode> googleNodes, boolean backPresed,boolean loadMore) {
        this.googleNodes = googleNodes;
        this.backPresed = backPresed;
        this.loadMore = loadMore;
    }

    public boolean isLoadMore() {
        return loadMore;
    }

    public boolean isBackPresed() {
        return backPresed;
    }

    public ArrayList<GoogleDriveNode> getGoogleNodes() {
        return googleNodes;
    }

}
