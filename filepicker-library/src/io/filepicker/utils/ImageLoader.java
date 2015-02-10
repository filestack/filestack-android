package io.filepicker.utils;

import android.content.Context;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;

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

        OkHttpClient fpHttpClient = new OkHttpClient();
        fpHttpClient.networkInterceptors().add(new FpSessionedInterceptor(context));
        builder.downloader(new OkHttpDownloader(fpHttpClient));
        return builder.build();
    }

    // Interceptor which has filepicker cookie session set
    private static class FpSessionedInterceptor implements Interceptor {

        Context mContext;

        FpSessionedInterceptor(Context context) {
            mContext = context;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            Request sessionedRequest = request.newBuilder()
                    .header("Cookie", "session=" +
                            PreferencesUtils.newInstance(mContext).getSessionCookie())
                    .build();
            return chain.proceed(sessionedRequest);
        }
    }
}
