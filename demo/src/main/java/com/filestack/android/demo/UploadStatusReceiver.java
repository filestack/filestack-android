package com.filestack.android.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import com.filestack.FileLink;
import com.filestack.android.FsConstants;
import com.filestack.android.Selection;

import java.util.Locale;

public class UploadStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "UploadStatusReceiver";

    private TextView logView;

    public UploadStatusReceiver(TextView logView) {
        this.logView = logView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = intent.getStringExtra(FsConstants.EXTRA_STATUS);
        Selection selection = intent.getParcelableExtra(FsConstants.EXTRA_SELECTION);
        FileLink fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);
        String name = selection.getName();
        String handle = fileLink != null ? fileLink.getHandle() : "n/a";

        logView.append("========================\n");
        logView.append(status.toUpperCase() + "\n");
        logView.append(name + "\n");
        logView.append("https://cdn.filestackcontent.com/" + handle + "\n");
        logView.append("========================\n");
    }
}
