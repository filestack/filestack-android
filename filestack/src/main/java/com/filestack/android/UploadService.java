package com.filestack.android;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.filestack.FileLink;
import com.filestack.Sources;
import com.filestack.StorageOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class UploadService extends IntentService {
    public static final String SERVICE_NAME = "uploadService";
    public static final String PREF_NOTIFY_ID_COUNTER = "notifyIdCounter";
    public static final String TAG = "uploadService";

    public UploadService() {
        super(SERVICE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onHandleIntent(Intent intent) {
        ArrayList<Selection> selections = (ArrayList<Selection>)
                intent.getSerializableExtra(FsConstants.EXTRA_SELECTION_LIST);
        StorageOptions storeOpts = (StorageOptions)
                intent.getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);

        SharedPreferences prefs = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        int notifyId = prefs.getInt(PREF_NOTIFY_ID_COUNTER, 0);
        int total = selections.size();

        int i = 0;
        for (Selection item : selections) {
            String name = item.getName();
            String provider = item.getProvider();

            Log.d(TAG, "received: " + provider + " " + name);

            FileLink fileLink;
            if (isLocal(item)) {
                fileLink = uploadLocal(item, storeOpts);
            } else {
                fileLink = uploadCloud(item, storeOpts);
            }

            updateNotification(notifyId, ++i, total, name);
            sendBroadcast(item, fileLink);
        }

        prefs.edit().putInt(PREF_NOTIFY_ID_COUNTER, notifyId+1).apply();
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

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        String id = FsConstants.NOTIFY_CHANNEL_UPLOAD;
        CharSequence name = getString(R.string.notify_channel_upload_name);
        String description = getString(R.string.notify_channel_upload_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    private void updateNotification(int id, int done, int total, String name) {
        Locale locale = Locale.getDefault();
        String channelId = FsConstants.NOTIFY_CHANNEL_UPLOAD;
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        if (total == done) {
            builder.setContentTitle(String.format(locale, "Uploaded %d files", done));
            builder.setSmallIcon(R.drawable.ic_menu_upload_done_white);
        } else {
            builder.setContentTitle(String.format(locale, "Uploading %d/%d files", done, total));
            builder.setSmallIcon(R.drawable.ic_menu_upload_white);
            builder.setContentText(name);
            builder.setProgress(total, done, false);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id, builder.build());
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
