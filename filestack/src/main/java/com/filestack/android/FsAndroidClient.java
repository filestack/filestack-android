package com.filestack.android;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.filestack.FsClient;
import com.filestack.FsFile;
import com.filestack.Progress;
import com.filestack.StorageOptions;

import java.io.IOException;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FsAndroidClient extends FsClient {

    /**
     * Builds new {@link FsAndroidClient}.
     */
    public static class Builder extends FsClient.Builder<Builder> {

        /**
         * Create an {@link FsAndroidClient} using the configured values.
         */
        public FsAndroidClient build() {
            obsScheduler = obsScheduler != null ? obsScheduler : AndroidSchedulers.mainThread();
            super.build();
            return new FsAndroidClient(this);
        }
    }

    protected FsAndroidClient(Builder builder) {
        super(builder);
    }

    /**
     * Uploads {@link Uri} using default storage options.
     *
     * @see #upload(String, boolean, StorageOptions)
     */
    public FsFile upload(Context context, Uri uri, boolean intelligent) throws IOException {
        return super.upload(getPathFromMediaUri(context, uri), intelligent);
    }

    /**
     * Uploads {@link Uri}.
     *
     * @see #upload(String, boolean, StorageOptions)
     */
    public FsFile upload(Context context, Uri uri, boolean intelligent, StorageOptions options)
            throws IOException {
        return super.upload(getPathFromMediaUri(context, uri), intelligent, options);
    }

    /**
     * Asynchronously uploads {@link Uri} using default storage options.
     *
     * @see #upload(String, boolean, StorageOptions)
     * @see #uploadAsync(String, boolean, StorageOptions)
     */
    public Flowable<Progress<FsFile>> uploadAsync(Context context, Uri uri, boolean intelligent) {
        return super.uploadAsync(getPathFromMediaUri(context, uri), intelligent, null);
    }

    /**
     * Asynchronously uploads {@link Uri}.
     *
     * @see #upload(String, boolean, StorageOptions)
     * @see #uploadAsync(String, boolean, StorageOptions)
     */
    public Flowable<Progress<FsFile>> uploadAsync(Context context, Uri uri, boolean intelligent,
                                                    StorageOptions options) {
        return super.uploadAsync(getPathFromMediaUri(context, uri), intelligent, options);
    }

    private static String getPathFromMediaUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(uri,  projection, null, null, null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    interface Provider {
        FsAndroidClient getClient();
    }
}
