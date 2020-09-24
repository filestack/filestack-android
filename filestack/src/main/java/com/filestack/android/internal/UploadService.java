package com.filestack.android.internal;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.ServiceCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.filestack.FileLink;
import com.filestack.Sources;
import com.filestack.StorageOptions;
import com.filestack.android.FsConstants;
import com.filestack.android.R;
import com.filestack.android.Selection;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.internal.Internal;

/**
 * If the auto upload option is left enabled, a user's selections will be sent to this service
 * when {@link com.filestack.android.FsActivity} is closed (upload button is clicked). In this
 * service we upload files (or makes calls for cloud to cloud transfers) and send up notification
 * messages on the progress.
 * TODO Use async version of Java SDK upload so we can show incremental progress for large uploads
 */
public class UploadService extends Service {

    private static final String NOTIFY_CHANNEL_UPLOAD = "uploadsChannel";

    private Executor executor = Executors.newSingleThreadExecutor();

    private NotificationManager notificationManager;
    private int notificationId;
    private int errorNotificationId;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationId = UUID.randomUUID().hashCode();
        errorNotificationId = notificationId + 1;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final ArrayList<Selection> selections = intent.getParcelableArrayListExtra(FsConstants.EXTRA_SELECTION_LIST);
        StorageOptions storeOpts = (StorageOptions) intent.getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);
        if (storeOpts == null) {
            storeOpts = new StorageOptions.Builder().build();
        }

        Notification serviceNotification =
                progressNotification(0, selections.size(), "").build();
        startForeground(notificationId, serviceNotification);

        final StorageOptions storageOptions = storeOpts;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                uploadFiles(selections, storageOptions);
                stopSelf();
            }
        });
        return START_NOT_STICKY;
    }

    private void uploadFiles(List<Selection> selections, StorageOptions storeOpts) {
        int total = selections.size();

        int i = 0;
        for (Selection item : selections) {
            String name = item.getName();

            sendProgressNotification(i, total, name);
            FileLink fileLink = upload(item, storeOpts);

            // If upload fails, decrease total count and show error notification
            if (fileLink == null) {
                sendErrorNotification(item.getName());
                total--;
            } else {
                i++;
            }

            sendProgressNotification(i, total, name);
            sendBroadcast(item, fileLink);
        }
    }

    private FileLink upload(Selection selection, StorageOptions baseOptions) {
        String provider = selection.getProvider();
        String path = selection.getPath();
        Uri uri = selection.getUri();
        int size = selection.getSize();
        String name = selection.getName();
        String mimeType = selection.getMimeType();

        StorageOptions options = baseOptions.newBuilder()
                .filename(baseOptions.getFilename() == null ? baseOptions.getFilename(): name)
                .mimeType(mimeType)
                .build();

        try {
            switch (selection.getProvider()) {
                case Sources.CAMERA:
                    // TODO This should maybe be unified into an InputStream upload
                    return Util.getClient().upload(path, false, options);
                case Sources.DEVICE:
                    InputStream input = getContentResolver().openInputStream(uri);
                    return Util.getClient().upload(input, size, false, options);
                default:
                    return Util.getClient().storeCloudItem(provider, path, options);
            }
        } catch (Exception e) {
            // TODO Update after fixing synchronous versions of upload methods in Java SDK
            // Currently these are "block mode" observables and don't properly pass up exceptions
            // correctly among other issues
            e.printStackTrace();
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        CharSequence name = getString(R.string.filestack__notify_channel_upload_name);
        String description = getString(R.string.filestack__notify_channel_upload_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel =
                new NotificationChannel(NOTIFY_CHANNEL_UPLOAD, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    private void sendProgressNotification(int done, int total, String name) {
        if (total == 0) {
            notificationManager.cancel(notificationId);
            return;
        }
        NotificationCompat.Builder builder;
        if (total == done) {
            builder = new NotificationCompat.Builder(this, NOTIFY_CHANNEL_UPLOAD);
            builder.setContentTitle(String.format(Locale.getDefault(), "Uploaded %d files", done));
            builder.setSmallIcon(R.drawable.filestack__ic_menu_upload_done_white);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        } else {
            builder = progressNotification(done, total, name);
        }
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationManager.notify(notificationId, builder.build());
    }

    private NotificationCompat.Builder progressNotification(int done, int total, String currentFileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFY_CHANNEL_UPLOAD);
        builder.setContentTitle(String.format(Locale.getDefault(), "Uploading %d/%d files", done, total));
        builder.setSmallIcon(R.drawable.filestack__ic_menu_upload_white);
        builder.setContentText(currentFileName);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setProgress(total, done, false);
        return builder;
    }

    private void sendErrorNotification(String name) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFY_CHANNEL_UPLOAD);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentTitle("Upload failed");
        builder.setContentText(name);
        builder.setSmallIcon(R.drawable.filestack__ic_menu_upload_fail_white);

        notificationManager.notify(errorNotificationId, builder.build());
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
