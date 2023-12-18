package com.filestack.android.demo;

import static com.filestack.android.FsConstants.STATUS_IN_PROGRESS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.filestack.FileLink;
import com.filestack.android.FsConstants;
import com.filestack.android.Selection;

import java.util.Locale;

public class UploadStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "UploadStatusReceiver";

    private TextView logView;
    private ScrollView scrollView;

    public UploadStatusReceiver(TextView logView, ScrollView scrollView) {
        this.logView = logView;
        this.scrollView = scrollView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = intent.getStringExtra(FsConstants.EXTRA_STATUS);
        Selection selection = intent.getParcelableExtra(FsConstants.EXTRA_SELECTION);
        FileLink fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);
        String name = selection.getName();
        logView.append("========================\n");
        logView.append(status.toUpperCase() + "\n");
        logView.append(name + "\n");
        if (status.equals(STATUS_IN_PROGRESS)) {
            double percent = intent.getDoubleExtra(FsConstants.EXTRA_PERCENT, 100);
            logView.append(String.format("Percentage completed: %f%%\n", percent * 100));
        } else {
            String handle = fileLink != null ? fileLink.getHandle() : "n/a";
            logView.append("https://cdn.filestackcontent.com/" + handle + "\n");
        }
        logView.append("========================\n");
        scrollDown();
    }

    private void scrollDown() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
