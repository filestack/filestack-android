package com.filestack.android.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.filestack.FileLink;
import com.filestack.android.FsConstants;
import com.filestack.android.Selection;

public class UploadStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String status = intent.getStringExtra(FsConstants.EXTRA_STATUS);
        Selection selection = (Selection) intent.getSerializableExtra(FsConstants.EXTRA_SELECTION);
        FileLink fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);

        Log.i("uploadStatus", selection.getName() + " " + status + " "
                + (fileLink != null ? fileLink.getHandle() : ""));
    }
}
