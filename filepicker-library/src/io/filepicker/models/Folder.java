package io.filepicker.models;


import com.google.gson.annotations.SerializedName;

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
