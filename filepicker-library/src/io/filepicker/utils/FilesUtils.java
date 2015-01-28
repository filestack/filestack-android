package io.filepicker.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.filepicker.models.Node;
import retrofit.mime.TypedFile;

/**
 * Created by maciejwitowski on 11/5/14.
 */
public class FilesUtils {

    public static TypedFile getTypedFileFromUri(Context context, Uri uri) {
        String mimetype;
        if("content".equalsIgnoreCase(uri.getScheme())) {
            mimetype = context.getContentResolver().getType(uri);
        } else {
            mimetype = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
        }

        if(mimetype == null) {
            return null;
        }

        String path = getPath(context, uri);
        // File path and mimetype are required
        if(path == null)
            return null;

        File file = new File(path);

        return new TypedFile(mimetype, file);
    }

    public static String getFileExtension(Context context, Uri uri) {
        ContentResolver cR = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    @SuppressWarnings("NewApi")
    private static String getPath(final Context context, final Uri uri) {

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

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
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
    private static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
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
        if(!path.endsWith("/"))
            path += "/";

        path += filename + "." + fileExtension;

        return path;
    }

    /**
     * Opens file from uri for reading and write its content to temporary file in cache directory
     * @param context
     * @param fileUri - uri of file to save
     * @return - built TypedFile
     */
    public static TypedFile buildTypedFile(Context context, Uri fileUri) {
        TypedFile typedFile = null;
        ContentResolver cr = context.getContentResolver();

        try {
            ParcelFileDescriptor descriptor = cr.openFileDescriptor(fileUri, "r");
            InputStream is = new FileInputStream(descriptor.getFileDescriptor());

            File outputFile = File.createTempFile("prefix", "extension", context.getCacheDir());
            OutputStream os = new FileOutputStream(outputFile);

            outputFile.setWritable(true);


            byte[] buffer = new byte[1024];
            int length;

            while((length = is.read(buffer)) > 0){
                os.write(buffer, 0, length);
            }

            typedFile = new TypedFile(cr.getType(fileUri), outputFile);
            os.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return typedFile;
    }

}
