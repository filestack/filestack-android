package io.filepicker.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by maciejwitowski on 1/13/15.
 */
// Node with a view type indicating how to display it
public class DisplayedNode implements Parcelable {
    public Node node;
    public String viewType;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.node, 0);
        dest.writeString(this.viewType);
    }

    public DisplayedNode() {
    }

    public DisplayedNode(Node node) {
        this.node = node;
    }

    public DisplayedNode(Node node, String viewType) {
        this.node = node;
        this.viewType = viewType;
    }

    private DisplayedNode(Parcel in) {
        this.node = in.readParcelable(Node.class.getClassLoader());
        this.viewType = in.readString();
    }

    public static final Parcelable.Creator<DisplayedNode> CREATOR = new Parcelable.Creator<DisplayedNode>() {
        public DisplayedNode createFromParcel(Parcel source) {
            return new DisplayedNode(source);
        }

        public DisplayedNode[] newArray(int size) {
            return new DisplayedNode[size];
        }
    };
}
