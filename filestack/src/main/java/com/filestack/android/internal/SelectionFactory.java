package com.filestack.android.internal;

import android.net.Uri;

import com.filestack.CloudItem;
import com.filestack.Sources;
import com.filestack.android.Selection;

class SelectionFactory {

    public static Selection from(String sourceId, CloudItem cloudItem) {
        return new Selection(sourceId, cloudItem.getPath(), cloudItem.getMimetype(),
                cloudItem.getName());
    }

    public static Selection from(Uri uri, int size, String mimeType, String name) {
        return new Selection(Sources.DEVICE, uri, size, mimeType, name);
    }

}
