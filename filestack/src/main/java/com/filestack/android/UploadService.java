package com.filestack.android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.filestack.FileLink;
import com.filestack.Sources;
import com.filestack.StorageOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class UploadService extends IntentService {
    public static final String SERVICE_NAME = "Filestack Upload Service";
    public static final String PREF_ID_COUNTER = "idCounter";

    public UploadService() {
        super(SERVICE_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onHandleIntent(Intent intent) {
        ArrayList<Selection> selections;
        ArrayList<Selection> localItems;

        SharedPreferences preferences = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        int id = preferences.getInt(PREF_ID_COUNTER, 0);

        selections = (ArrayList<Selection>) intent.getSerializableExtra(FsConstants.EXTRA_SELECTION_LIST);
        localItems = new ArrayList<>();

        for (Selection item : selections) {
            Log.d("uploadService", "received: " + item.getProvider() + " " + item.getPath());

            // Separate local items into their own list
            // Want to upload cloud items first since that goes faster
            if (isLocal(item)) {
                selections.remove(item);
                localItems.add(item);
            }
        }

        StorageOptions storeOpts =
                (StorageOptions) intent.getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);

        int count = 0;
        for (Selection selection : selections) {
            FileLink fileLink = uploadCloud(selection, storeOpts);
            updateNotification(id, selections, ++count);
            sendBroadcast(selection, fileLink);
        }

        for (Selection selection : localItems) {
            FileLink fileLink = uploadLocal(selection, storeOpts);
            updateNotification(id, selections, ++count);
            sendBroadcast(selection, fileLink);
        }

        preferences.edit().putInt(PREF_ID_COUNTER, id+1).apply();
    }

    private boolean isLocal(Selection item) {
        switch (item.getProvider()) {
            case Sources.CAMERA:
            case Sources.DEVICE:
                return true;
            default:
                return false;
        }
    }

    private FileLink uploadLocal(Selection item, StorageOptions storeOpts) {
        try {
            return Util.getClient().upload(item.getPath(), false, storeOpts);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private FileLink uploadCloud(Selection item, StorageOptions storeOpts) {
        try {
            return Util.getClient().storeCloudItem(item.getProvider(), item.getPath(), storeOpts);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateNotification(int id, ArrayList<Selection> selections, int count) {
        int total = selections.size();
        Locale locale = Locale.getDefault();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        if (total == count) {
            mBuilder.setContentTitle(String.format(locale, "Uploaded %d files", count));
            mBuilder.setSmallIcon(R.drawable.ic_menu_upload_done_white);
        } else {
            mBuilder.setContentTitle(String.format(locale, "Uploading %d/%d files", count, total));
            mBuilder.setSmallIcon(R.drawable.ic_menu_upload_white);
            mBuilder.setContentText(selections.get(count-1).getName());
            mBuilder.setProgress(total, count, false);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(id, mBuilder.build());
    }

    private void sendBroadcast(Selection selection, FileLink fileLink) {
        Intent intent = new Intent(FsConstants.BROADCAST_UPLOAD);
        intent.putExtra(FsConstants.EXTRA_SELECTION, selection);
        if (fileLink == null) {
            intent.putExtra(FsConstants.EXTRA_STATUS, FsConstants.STATUS_FAILED);
        } else {
            intent.putExtra(FsConstants.EXTRA_STATUS, FsConstants.STATUS_COMPLETE);
        }
        intent.putExtra(FsConstants.EXTRA_FILE_LINK, fileLink);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
