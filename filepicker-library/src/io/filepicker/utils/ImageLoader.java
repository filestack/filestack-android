package io.filepicker.utils;

import android.content.Context;
import android.net.Uri;

import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by maciejwitowski on 10/29/14.
 */
public class ImageLoader {

    private static Picasso imageLoader;

    public static Picasso getImageLoader(Context context) {
        if (imageLoader == null) {
            imageLoader = buildImageLoader(context);
        }
        return imageLoader;
    }

    public static void setImageLoader(Context context) {
        imageLoader = buildImageLoader(context);
    }

    // Return Picasso object with the session set, so Picasso can fetch thumbnails from Filepicker Api
    public static Picasso buildImageLoader(final Context context) {
        Picasso.Builder builder = new Picasso.Builder(context);

        builder.downloader(new OkHttpDownloader(context) {
            @Override
            protected HttpURLConnection openConnection(Uri uri) throws IOException {
                HttpURLConnection connection = super.openConnection(uri);
                connection.setRequestProperty("Cookie", "session=" +
                        PreferencesUtils.newInstance(context).getSessionCookie());

                return connection;
            }
        });
        return builder.build();
    }
}
