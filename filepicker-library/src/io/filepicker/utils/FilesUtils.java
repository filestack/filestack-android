package io.filepicker.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.filepicker.models.Node;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by maciejwitowski on 11/5/14.
 */
public class FilesUtils {

    public static RequestBody getRequestBodyFromUri(Context context, String path, Uri uri) {
        String mimetype = getMimeType(context, uri);
        if (mimetype == null) {
            return null;
        }

        // File path and mimetype are required
        if (path == null) {
            return null;
        }

        return RequestBody.create(MediaType.parse(mimetype), new File(path));
    }

    private static String getMimeType(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return context.getContentResolver().getType(uri);
        } else {
            String path = uri.getPath();
            path = path.replaceAll("[^a-zA-Z_0-9\\.\\-\\(\\)\\%]", "");
            String extension = MimeTypeMap.getFileExtensionFromUrl(path);
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
    }

    public static String getFileExtension(Context context, Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(resolver.getType(uri));
    }

    public static String getPath(final Context context, final Uri uri) {
        String path = readPathFromUri(context, uri);
        if (path == null || path.startsWith("http")) {
            File cachedFile = cacheRemoteFile(context, uri);
            if (cachedFile != null && cachedFile.exists()) {
                path = cachedFile.getPath();
            }
        }
        return path;
    }

    @SuppressWarnings("NewApi")
    private static String readPathFromUri(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = getContentUri(type);

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static Uri getContentUri(String type) {
        switch (type) {
            case "image":
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            case "video":
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            case "audio":
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA;
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static File cacheRemoteFile(Context context, Uri uri) {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap != null) {
                File file = new File(context.getCacheDir(), "image");
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
                return file;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /** Return combined path to file
     *
     * @param node - parent node
     * @param filename - name of file
     * @param fileExtension - extension of file
     * @return String - path to file
     */
    public static String getFilePath(Node node, String filename, String fileExtension) {
        String path = node.linkPath;
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += filename + "." + fileExtension;
        return path;
    }

    /**
     * Opens file from uri for reading and write its content to temporary file in cache directory
     * @param context
     * @param fileUri - uri of file to save
     * @return - built TypedFile
     */
    public static RequestBody buildRequestBody(Context context, Uri fileUri) {
        RequestBody typedFile = null;
        ContentResolver cr = context.getContentResolver();

        try {
            ParcelFileDescriptor descriptor = cr.openFileDescriptor(fileUri, "r");
            InputStream is = new FileInputStream(descriptor.getFileDescriptor());

            File outputFile = File.createTempFile("prefix", "extension", context.getCacheDir());
            OutputStream os = new FileOutputStream(outputFile);

            outputFile.setWritable(true);

            byte[] buffer = new byte[1024];
            int length;

            while((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            typedFile = RequestBody.create(MediaType.parse(cr.getType(fileUri)), outputFile);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return typedFile;
    }

}
