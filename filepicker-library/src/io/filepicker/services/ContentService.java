package io.filepicker.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.filepicker.ExportFragment;
import io.filepicker.Filepicker;
import io.filepicker.api.FpApiClient;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FileExportedEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GetContentEvent;
import io.filepicker.events.SignedOutEvent;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.Node;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.Constants;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.PreferencesUtils;
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
    public static final String ACTION_EXPORT_FILE = "io.filepicker.services.action.export_file";

    public static final String EXTRA_NODE = "io.filepicker.services.extra.node";
    public static final String EXTRA_FILENAME = "io.filepicker.services.extra.filename";

    // Used for upload file action and uri looks like content://<path to local file>
    public static final String EXTRA_FILE_URI = "io.filepicker.services.extra.file_uri";

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

    public static void exportFile(Context context, Node node, Uri fileUri, String filename) {
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_EXPORT_FILE);
        intent.putExtra(EXTRA_NODE, node);
        intent.putExtra(EXTRA_FILENAME, filename);
        intent.putExtra(EXTRA_FILE_URI, fileUri);
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
            } else if (ACTION_EXPORT_FILE.equals(action)) {
                Node node = intent.getParcelableExtra(EXTRA_NODE);
                String filename = intent.getStringExtra(EXTRA_FILENAME);
                Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
                handleActionExportFile(node, fileUri, filename);
            }
        }
    }

    private void handleActionGetContent(Node node) {
        Log.d(LOG_TAG, "handleActionGetContent for path " + node.getLinkPath());
        FpApiClient.getFpApiClient(this)
                .getFolder(node.getLinkPath(), "info",
                    FpApiClient.getJsSession(this),
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
            FPFile result = FpApiClient.getFpApiClient(this).pickFile(
                    node.getLinkPath(),
                    "fpurl",
                    FpApiClient.getJsSession(this));

            results.add(result);
        }

        EventBus.getDefault().post(new FpFilesReceivedEvent(results));
    }

    private void handleActionUploadFile(final Uri uri) {
        FpApiClient.getFpApiClient(this)
                .uploadFile(Utils.getImageName(),
                        FpApiClient.getJsSession(this),
                        FilesUtils.getTypedFileFromUri(this, uri),
                        new Callback<UploadLocalFileResponse>() {
                            @Override
                            public void success(UploadLocalFileResponse object, retrofit.client.Response response) {
                                ArrayList<FPFile> fpFiles = new ArrayList<FPFile>();

                                FPFile fpFile = object.parseToFpFile();
                                fpFile.setLocalPath(uri.toString());
                                fpFiles.add(fpFile);

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
                        FpApiClient.getJsSession(this),

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

    /** Exports file to service
     node - destination node
     fileUri - uri to file on device
     filename - new filename given by user
    */
    private void handleActionExportFile(Node node, Uri fileUri, String filename) {

        String fileExtension = FilesUtils.getFileExtension(this, fileUri);
        final String path = FilesUtils.getFilePath(node, filename, fileExtension);
        TypedFile content = FilesUtils.buildTypedFile(this, fileUri);

        FpApiClient.getFpApiClient(this)
            .exportFile(path, FpApiClient.getJsSession(this), content,
                    new Callback<FPFile>() {
                        @Override
                        public void success(FPFile fpFile, Response response) {
                            EventBus.getDefault().post(new FileExportedEvent(path, fpFile));
                            Log.d(LOG_TAG, "success");
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.d(LOG_TAG, "failure");
                        }
                    });
    }

    private void handleError(RetrofitError error) {
        EventBus.getDefault().post(new ApiErrorEvent(error));
    }
}

