package io.filepicker.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.filepicker.Filepicker;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GetContentEvent;
import io.filepicker.events.SignedOutEvent;
import io.filepicker.models.FPFile;
import io.filepicker.models.Node;
import io.filepicker.api.FpApiClient;
import io.filepicker.models.Folder;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.Utils;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class ContentService extends IntentService {

    private static final String LOG_TAG = ContentService.class.getSimpleName();

    public static final String ACTION_GET_CONTENT = "io.filepicker.services.action.get_content";
    public static final String ACTION_UPLOAD_FILE = "io.filepicker.services.action.upload_file";
    public static final String ACTION_PICK_FILES = "io.filepicker.services.action.pick_files";
    public static final String ACTION_LOGOUT = "io.filepicker.services.action.logout";

    public static final String EXTRA_NODE = "io.filepicker.services.extra.node";

    // Used for upload file action and uri looks like content://<path to local file>
    public static final String EXTRA_FILE_URI = "io.filepicker.services.extra.file_uri";

    // Used for pick file action and path looks like <name of service>/<path to file>
//    public static final String EXTRA_FILE_PATH = "io.filepicker.services.extra.file_path";

    public ContentService() {
        super("ContentService");
    }

    public static void getContent(Context context, Node node) {
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_GET_CONTENT);
        intent.putExtra(EXTRA_NODE, node);
        context.startService(intent);
    }

    public static void pickFiles(Context context, ArrayList<Node> files) {
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_PICK_FILES);
        intent.putParcelableArrayListExtra(EXTRA_NODE, files);
        context.startService(intent);
    }

    public static void uploadFile(Context context, Uri fileUri) {
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_UPLOAD_FILE);
        intent.putExtra(EXTRA_FILE_URI, fileUri);
        context.startService(intent);
    }

    public static void logout(Context context, Node node) {
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_LOGOUT);
        intent.putExtra(EXTRA_NODE, node);
        context.startService(intent);
    }

    @Override
    protected  void onHandleIntent(Intent intent) {
        if(intent != null) {
            final String action = intent.getAction();

            if(ACTION_GET_CONTENT.equals(action)) {
                Node node = intent.getParcelableExtra(EXTRA_NODE);
                handleActionGetContent(node);
            } else if (ACTION_UPLOAD_FILE.equals(action)) {
                Uri uri = intent.getParcelableExtra(EXTRA_FILE_URI);
                handleActionUploadFile(uri);
            } else if (ACTION_PICK_FILES.equals(action)) {
                ArrayList<Node> files = intent.getParcelableArrayListExtra(EXTRA_NODE);
                handleActionPickFiles(files);
            } else if (ACTION_LOGOUT.equals(action)) {
                Node node = intent.getParcelableExtra(EXTRA_NODE);
                handleActionLogout(node);
            }
        }
    }

    private void handleActionGetContent(Node node) {
        Log.d(LOG_TAG, "handleActionGetContent for path " + node.getLinkPath());
        FpApiClient.getFpApiClient(this)
                .getFolder(node.getLinkPath(), "info",
                        FpApiClient.buildJsSession(Filepicker.getApiKey(), ""),
                        new Callback<Folder>() {
                            @Override
                            public void success(Folder folder, retrofit.client.Response response) {
                                EventBus.getDefault().post(new GetContentEvent(folder));
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                handleError(error);
                            }
                        });
    }

    private void handleActionPickFiles(ArrayList<Node> nodes) {
        final ArrayList<FPFile> results = new ArrayList<FPFile>();

        for(Node node : nodes) {
            FPFile result = FpApiClient.getFpApiClient(this).pickFile(node.getLinkPath(),
                    "fpurl", FpApiClient.buildJsSession(Filepicker.getApiKey(), ""));

            results.add(result);
        }

        EventBus.getDefault().post(new FpFilesReceivedEvent(results));
    }

    private void handleActionUploadFile(Uri uri) {
        FpApiClient.getFpApiClient(this)
                .uploadFile(FpApiClient.buildJsSession(Filepicker.getApiKey(), ""),
                        getTypedFileFromUri(uri),
                        new Callback<UploadLocalFileResponse>() {
                            @Override
                            public void success(UploadLocalFileResponse object, retrofit.client.Response response) {
                                ArrayList<FPFile> fpFiles = new ArrayList<FPFile>();
                                fpFiles.add(object.parseToFpFile());

                                EventBus.getDefault().post(new FpFilesReceivedEvent(fpFiles));
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                handleError(error);
                            }
                        });
    }

    private void handleActionLogout (Node node) {
        FpApiClient.getFpApiClient(this)
                .logout(node.getDisplayName().toLowerCase(),
                        FpApiClient.buildJsSession(Filepicker.getApiKey(), ""),
                        new Callback<Object>() {
                            @Override
                            public void success(Object fpFile, Response response) {
                                EventBus.getDefault().post(new SignedOutEvent());
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                handleError(error);
                            }
                        });
    }

    private TypedFile getTypedFileFromUri(Uri uri) {
        File file = new File(getRealPathFromURI(uri));
        String mimetype = Utils.MIMETYPE_IMAGE;

        return new TypedFile(mimetype, file);
    }

    private void handleError(RetrofitError error) {
        EventBus.getDefault().post(new ApiErrorEvent(error));
    }

    private byte[] readBinaryInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
