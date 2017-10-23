package com.filestack.android;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.filestack.FsFile;
import com.filestack.FsClient;
import com.filestack.Progress;
import com.filestack.Security;
import com.filestack.StorageOptions;
import com.filestack.util.FsService;
import io.reactivex.Flowable;
import java.io.IOException;

public class FsAndroidClient extends FsClient {

    /**
     * Constructs a client without security.
     *
     * @param apiKey account key from the dev portal
     */
    public FsAndroidClient(String apiKey) {
        super(apiKey);
    }

    /**
     * Constructs a client with security.
     *
     * @param apiKey   account key from the dev portal
     * @param security configured security object
     */
    public FsAndroidClient(String apiKey, Security security) {
        super(apiKey, security);
    }

    /**
     * Constructs a client using custom {@link FsService}. For internal use.
     *
     * @param apiKey    account key from the dev portal
     * @param security  configured security object
     * @param fsService service to use for API calls, overrides default singleton
     */
    public FsAndroidClient(String apiKey, Security security, FsService fsService) {
        super(apiKey, security, fsService);
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
