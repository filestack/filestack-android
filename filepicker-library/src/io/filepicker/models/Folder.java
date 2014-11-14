package io.filepicker.models;


import android.provider.ContactsContract;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by maciejwitowski on 10/24/14.
 */
public class Folder {

    String client;
    String view;
    boolean auth = true;

    @SerializedName(value="contents")
    Node[] nodes;

    public boolean isAuthorized() {
        return auth == true;
    }

    public String getClient() {
        return client;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public String getViewType() {
        return view;
    }
}