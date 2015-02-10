package io.filepicker.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import io.filepicker.R;
import io.filepicker.utils.Utils;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class Node implements Parcelable {

    @SerializedName(value="display_name")
    public String displayName;

    @SerializedName(value="link_path")
    public String linkPath;

    @SerializedName(value="is_dir")
    public boolean isDir;

    @SerializedName(value="thumb_exists")
    public boolean thumbExists = false;

    @SerializedName(value="thumbnail")
    public String thumbnailUrl;
    public int imageResource;

    public Node() {}

    public Node(String displayName, String linkPath, boolean isDir, int imageResource) {
        this.displayName = displayName;
        this.linkPath = linkPath;
        this.isDir = isDir;
        this.imageResource = imageResource;
    }

    public int getImageResource() {
        if(imageResource != 0) {
            return imageResource;
        }
        else {
            if (isDir) {
                return R.drawable.glyphicons_144_folder_open;
            } else {
                return R.drawable.glyphicons_036_file;
            }
        }
    }

    public boolean isImage() {
        if(linkPath == null || linkPath.equals(""))
            return false;

        // Facebook is special case since its linkPath doesn't contain file name
        if(Utils.belongsToImageOnlyProvider(this)) {
            return true;
        }

        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(linkPath);
        return fileExtension != null && Utils.isImage(fileExtension);
    }

    public boolean isCamera() {
        return this instanceof  Provider && this.displayName.equals("Camera");
    }

    public boolean isGallery() {
        return this instanceof Provider && this.displayName.equals("Gallery");
    }

    public boolean hasThumbnail() {
        return thumbExists && !thumbnailUrl.isEmpty();
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String deslashedPath() {
        return linkPath.replace("/", "_");
    }
    /** Used to give additional hints on how to process the received parcel.*/
    @Override
    public int describeContents() {
        // ignore for now
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeString(linkPath);
    }

    /** Static field userd to regenerate object, individually or as arrays */
    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel source) {
            return new Node(source);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[0];
        }
    };

    /** Creator for Parcel, reads back fields in the order they were written */
    public Node(Parcel pc) {
        displayName = pc.readString();
        linkPath    = pc.readString();
    }

    public static boolean nameExists(ArrayList<Node> nodes, String value) {
        boolean exists = false;
        for(Node node : nodes) {
            if(!node.isDir) {
                if(node.displayName.equals(value)) {
                    exists = true;
                    break;
                }
            }
        }

        return exists;
    }
}
