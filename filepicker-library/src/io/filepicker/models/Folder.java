package io.filepicker.models;


import com.google.gson.annotations.SerializedName;

/**
 * Created by maciejwitowski on 10/24/14.
 */
public final class Folder {

    public String client;
    public String view;
    public boolean auth = true;

    @SerializedName(value="contents")
    public Node[] nodes;
}