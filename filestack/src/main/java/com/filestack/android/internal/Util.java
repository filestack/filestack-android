package com.filestack.android.internal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v4.content.MimeTypeFilter;
import android.widget.TextView;

import com.filestack.Client;
import com.filestack.Config;
import com.filestack.Sources;
import com.filestack.android.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared, static utility methods. */
public class Util {
    private static final List<String> SOURCES_LIST = new ArrayList<>();
    private static final Map<String, SourceInfo> SOURCES_MAP = new HashMap<>();

    private static Client client;
    private static SelectionSaver selectionSaver;

    static {
        SOURCES_LIST.add(Sources.CAMERA);
        SOURCES_LIST.add(Sources.DEVICE);
        SOURCES_LIST.add(Sources.GOOGLE_DRIVE);
        SOURCES_LIST.add(Sources.FACEBOOK);
        SOURCES_LIST.add(Sources.INSTAGRAM);
        SOURCES_LIST.add(Sources.DROPBOX);
        SOURCES_LIST.add(Sources.GOOGLE_PHOTOS);
        SOURCES_LIST.add(Sources.ONEDRIVE);
        SOURCES_LIST.add(Sources.GMAIL);
        SOURCES_LIST.add(Sources.BOX);
        SOURCES_LIST.add(Sources.AMAZON_DRIVE);
        SOURCES_LIST.add(Sources.GITHUB);
    }

    static {
        SOURCES_MAP.put(Sources.CAMERA, new SourceInfo(
                Sources.CAMERA,
                R.drawable.filestack__ic_source_camera,
                R.string.filestack__source_camera,
                R.color.filestack__theme_source_camera));

        SOURCES_MAP.put(Sources.DEVICE, new SourceInfo(
                Sources.DEVICE,
                R.drawable.filestack__ic_source_device,
                R.string.filestack__source_device,
                R.color.filestack__theme_source_device));

        SOURCES_MAP.put(Sources.FACEBOOK, new SourceInfo(
                Sources.FACEBOOK,
                R.drawable.filestack__ic_source_facebook,
                R.string.filestack__source_facebook,
                R.color.filestack__theme_source_facebook));

        SOURCES_MAP.put(Sources.GOOGLE_PHOTOS, new SourceInfo(
                Sources.GOOGLE_PHOTOS,
                R.drawable.filestack__ic_source_google_photos,
                R.string.filestack__source_google_photos,
                R.color.filestack__theme_source_google_photos));

        SOURCES_MAP.put(Sources.INSTAGRAM, new SourceInfo(
                Sources.INSTAGRAM,
                R.drawable.filestack__ic_source_instagram,
                R.string.filestack__source_instagram,
                R.color.filestack__theme_source_instagram));

        SOURCES_MAP.put(Sources.AMAZON_DRIVE, new SourceInfo(
                Sources.AMAZON_DRIVE,
                R.drawable.filestack__ic_source_amazon_drive,
                R.string.filestack__source_amazon_drive,
                R.color.filestack__theme_source_amazon_drive));

        SOURCES_MAP.put(Sources.BOX, new SourceInfo(
                Sources.BOX,
                R.drawable.filestack__ic_source_box,
                R.string.filestack__source_box,
                R.color.filestack__theme_source_box));

        SOURCES_MAP.put(Sources.DROPBOX, new SourceInfo(
                Sources.DROPBOX,
                R.drawable.filestack__ic_source_dropbox,
                R.string.filestack__source_dropbox,
                R.color.filestack__theme_source_dropbox));

        SOURCES_MAP.put(Sources.GOOGLE_DRIVE, new SourceInfo(
                Sources.GOOGLE_DRIVE,
                R.drawable.filestack__ic_source_google_drive,
                R.string.filestack__source_google_drive,
                R.color.filestack__theme_source_google_drive));

        SOURCES_MAP.put(Sources.ONEDRIVE, new SourceInfo(
                Sources.ONEDRIVE,
                R.drawable.filestack__ic_source_onedrive,
                R.string.filestack__source_onedrive,
                R.color.filestack__theme_source_onedrive));

        SOURCES_MAP.put(Sources.GITHUB, new SourceInfo(
                Sources.GITHUB,
                R.drawable.filestack__ic_source_github,
                R.string.filestack__source_github,
                R.color.filestack__theme_source_github));

        SOURCES_MAP.put(Sources.GMAIL, new SourceInfo(
                Sources.GMAIL,
                R.drawable.filestack__ic_source_gmail,
                R.string.filestack__source_gmail,
                R.color.filestack__theme_source_gmail));
    }

    public static List<String> getDefaultSources() {
        return new ArrayList<>(SOURCES_LIST.subList(0, 6));
    }

    public static int getSourceIntId(String stringId) {
        return SOURCES_LIST.indexOf(stringId) + 1;
    }

    public static String getSourceStringId(int intId) {
        return SOURCES_LIST.get(intId - 1);
    }

    public static SourceInfo getSourceInfo(String stringId) {
        return SOURCES_MAP.get(stringId);
    }

    public static void textViewReplace(TextView view, String target, String replacement) {
        String text = view.getText().toString();
        text = text.replace(target, replacement);
        view.setText(text);
    }

    /** Removes the last part of a file path. */
    public static String trimLastPathSection(String path) {
        String[] sections = path.split("/");
        StringBuilder newPath = new StringBuilder("/");
        for (int i = 1; i < sections.length - 1; i++) {
            newPath.append(sections[i]).append("/");
        }
        return newPath.toString();
    }

    // TODO The selection saving might be done in a better way, without a singleton
    public static SelectionSaver getSelectionSaver() {
        if (selectionSaver == null) {
            selectionSaver = new SimpleSelectionSaver();
        }
        return selectionSaver;
    }

    // TODO Paths used for media should be changed to system-wide folders instead of app-internal
    // Right now we're storing photos and videos to directories that are internal to the app
    // It would make more sense for media captured using the SDK to be saved like the camera app

    /** Create a file with a path appropriate to save a photo. Uses directory internal to app. */
    public static File createPictureFile(Context context) throws IOException {
        Locale locale = Locale.getDefault();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        // Store in normal camera directory
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    /** Create a file with a path appropriate to save a video. Uses directory internal to app. */
    public static File createMovieFile(Context context) throws IOException {
        Locale locale = Locale.getDefault();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(new Date());
        String fileName = "MP4_" + timeStamp + "_";
        // Store in normal camera directory
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        return File.createTempFile(fileName, ".mp4", storageDir);
    }

    // We need to get a URI from a file provider to avoid causing a FileUriExposedException
    // If we don't do this, we'll get the exception when sending the URI to the camera app
    // See the FileProvider example in https://developer.android.com/training/camera/photobasics
    public static Uri getUriForInternalMedia(Context context, File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = context.getPackageName() + ".fileprovider";
            uri =  FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    // TODO This doesn't seem to work
    /** Make media accessible outside the app. */
    public static void addMediaToGallery(Context context, String path) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /** Create the Java SDK client and set a session token. The token maintains cloud auth state. */
    public static void initializeClient(Config config, String sessionToken) {
        // Override returnUrl until introduction of FilestackUi class which will allow to set this
        // all up manually.
        Config overridenConfig = new Config(config.getApiKey(), "filestack://done",
                config.getPolicy(), config.getSignature());

        client = new Client(overridenConfig);
        client.setSessionToken(sessionToken);
    }

    public static Client getClient() {
        return client;
    }

    /** Returns true if the MIME type is allowed by the filters. */
    public static boolean mimeAllowed(String[] filters, String mimeType) {
        return MimeTypeFilter.matches(mimeType, filters) != null;
    }
}
