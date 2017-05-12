package io.filepicker.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;

import com.google.gson.annotations.SerializedName;

import io.filepicker.utils.Constants;
import io.filepicker.utils.Utils;

public class GoogleDriveNode extends Node implements Parcelable {

    @SerializedName(value="mime_type")
    public String mimeType;

    @SerializedName(value="drive_id")
    public String driveId;

    @SerializedName(value="icon_link")
    public String iconLink;

    @SerializedName(value="export_format")
    public String exportFormat = "";

    @SerializedName(value="drive_type")
    public String driveType;

    @Override
    public String deslashedPath() {
        return "drive_"+driveId;
    }

    @Override
    public String getThumbnailUrl() {
        if (!mimeType.contains("image"))
            return iconLink;

        return thumbnailUrl;
    }

    public GoogleDriveNode() {
    }


    @Override
    public boolean isImage() {
        if (mimeType.contains("image")) {
            return true;
        }

        return false;
    }

    // Parcelling part
    public GoogleDriveNode(Parcel in){
        String[] data = new String[9];
        in.readStringArray(data);
        // the order needs to be the same as in writeToParcel() method
        driveId = data[0];
        displayName = data[1];
        thumbExists = data[2].equals("true");
        iconLink = data[3];
        mimeType = data[4];
        thumbnailUrl = data[5];
        isDir = data[6].equals("true");
        exportFormat = data[7];
        driveType = data[8];
    }


    @Override
    public int describeContents() {
        // ignore for now
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {driveId,
                                            displayName,
                                            Boolean.valueOf(thumbExists).toString(),
                                            iconLink,
                                            mimeType,
                                            thumbnailUrl,
                                            Boolean.valueOf(isDir).toString(),
                                            exportFormat,
                                            driveType});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public GoogleDriveNode createFromParcel(Parcel in) {
            return new GoogleDriveNode(in);
        }

        public GoogleDriveNode[] newArray(int size) {
            return new GoogleDriveNode[size];
        }
    };
}
