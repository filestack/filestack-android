package io.filepicker.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.filepicker.api.FpApiClient;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FileExportedEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GotContentEvent;
import io.filepicker.events.UploadFileErrorEvent;
import io.filepicker.events.UploadProgressEvent;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.Node;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.Utils;

import java.util.concurrent.CountDownLatch;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class ContentService extends IntentService {

    private static final String LOG_TAG = ContentService.class.getSimpleName();

    private static final String ACTION_GET_CONTENT = "io.filepicker.services.action.get_content";
    private static final String ACTION_UPLOAD_FILE = "io.filepicker.services.action.upload_file";
    private static final String ACTION_PICK_FILES = "io.filepicker.services.action.pick_files";
    private static final String ACTION_EXPORT_FILE = "io.filepicker.services.action.export_file";

    private static final String EXTRA_BACK_PRESSED = "io.filepicker.services.extra.back_pressed";
    private static final String EXTRA_NODE = "io.filepicker.services.extra.node";
    private static final String EXTRA_FILENAME = "io.filepicker.services.extra.filename";

    // Used for upload file action and uri looks like content://<path to local file>
    private static final String EXTRA_FILE_URI = "io.filepicker.services.extra.file_uri";
    private static final int RETRY_COUNT = 10;
    private static final int RETRY_INTERVAL = 2000;

    FilepickerListener filepickerListener;

    public interface FilepickerListener {

        void onLocalFileUploaded(List<FPFile> files);

    }

    public ContentService() {
        super("ContentService");
    }

    public static void getContent(Context context, Node node, boolean backPressed) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_GET_CONTENT);
        intent.putExtra(EXTRA_NODE, node);
        intent.putExtra(EXTRA_BACK_PRESSED, backPressed);
        context.startService(intent);
    }

    public static void pickFiles(Context context, ArrayList<Node> files) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_PICK_FILES);
        intent.putParcelableArrayListExtra(EXTRA_NODE, files);
        context.startService(intent);
    }

    public static void uploadFile(Context context, Uri fileUri) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_UPLOAD_FILE);
        intent.putExtra(EXTRA_FILE_URI, fileUri);
        context.startService(intent);
    }

    public static void exportFile(Context context, Node node, Uri fileUri, String filename) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_EXPORT_FILE);
        intent.putExtra(EXTRA_NODE, node);
        intent.putExtra(EXTRA_FILENAME, filename);
        intent.putExtra(EXTRA_FILE_URI, fileUri);
        context.startService(intent);
    }

    public static void cancelAll() {
        FpApiClient.cancelAll();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            Node node;
            switch (action) {
                case ACTION_GET_CONTENT:
                    node = intent.getParcelableExtra(EXTRA_NODE);
                    boolean backPressed = intent.getBooleanExtra(EXTRA_BACK_PRESSED, false);
                    handleActionGetContent(node, backPressed);
                    break;
                case ACTION_UPLOAD_FILE:
                    Uri uri = intent.getParcelableExtra(EXTRA_FILE_URI);
                    handleActionUploadFile(uri);
                    break;
                case ACTION_PICK_FILES:
                    ArrayList<Node> files = intent.getParcelableArrayListExtra(EXTRA_NODE);
                    handleActionPickFiles(files);
                    break;
                case ACTION_EXPORT_FILE:
                    node = intent.getParcelableExtra(EXTRA_NODE);
                    String filename = intent.getStringExtra(EXTRA_FILENAME);
                    Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
                    handleActionExportFile(node, fileUri, filename);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleActionGetContent(Node node, final boolean backPressed) {
        FpApiClient.getFpApiClient(this)
            .getFolder(node.linkPath, "info", FpApiClient.getJsSession(this))
            .enqueue(new Callback<Folder>() {
                @Override
                public void onResponse(Call<Folder> call, Response<Folder> response) {
                    if (response.isSuccessful()) {
                        EventBus.getDefault().post(new GotContentEvent(response.body(), backPressed));
                    } else {
                        handleApiError(getErrorType(response));
                    }
                }

                @Override
                public void onFailure(Call<Folder> call, Throwable throwable) {
                    handleApiError(ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
                }
        });
    }

    private void handleActionPickFiles(ArrayList<Node> nodes) {
        final ArrayList<FPFile> results = new ArrayList<>();

        try {
            final CountDownLatch countDownLatch = new CountDownLatch(nodes.size());
            for (Node node : nodes) {
                FpApiClient.getFpApiClient(this)
                    .pickFile(URLDecoder.decode(node.linkPath, "utf-8"), "fpurl", FpApiClient.getJsSession(this))
                    .enqueue(new Callback<FPFile>() {
                        @Override
                        public void onResponse(Call<FPFile> call, Response<FPFile> response) {
                            if (response.isSuccessful()) {
                                results.add(response.body());
                            } else {
                                Log.w(LOG_TAG, "Error: " + response.code());
                            }
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Call<FPFile> call, Throwable throwable) {
                            Log.e(LOG_TAG, "Error", throwable);
                            countDownLatch.countDown();
                        }
                });


            }

            countDownLatch.await();
            if (results.isEmpty()) {
                Log.e(LOG_TAG, "handleActionPickFiles: Failed to retrieve FpFiles from the response");
                handleApiError(ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
            } else {
                EventBus.getDefault().post(new FpFilesReceivedEvent(results));
            }
        } catch (Exception syntaxException) {
            handleApiError(ApiErrorEvent.ErrorType.WRONG_RESPONSE);
        }
    }

    private void handleActionUploadFile(final Uri uri) {
        ApiErrorEvent.ErrorType errorType = null;
        RequestBody requestBody = null;

        String filePath = FilesUtils.getPath(this, uri);
        waitForFile(filePath);
        try {
            requestBody = FilesUtils.getRequestBodyFromUri(this, filePath, uri);
        } catch (SecurityException e) {
            errorType = ApiErrorEvent.ErrorType.LOCAL_FILE_PERMISSION_DENIAL;
        }

        if (requestBody == null && errorType == null) {
            errorType = ApiErrorEvent.ErrorType.INVALID_FILE;
        }

        if (errorType != null) {
            handleUploadFileError(uri, errorType);
            return;
        }

        File file = new File(filePath);
        requestBody = new ProgressRequestBody(file, uri.getPath(), new ProgressRequestBody.Listener() {
            @Override
            public void onProgress(float progress) {
                EventBus.getDefault().post(new UploadProgressEvent(uri, progress));
            }
        });

        FpApiClient.getFpApiClient(this).uploadFile(
                Utils.getUploadedFilename(file.getName()),
                FpApiClient.getJsSession(this),
                requestBody
        ).enqueue(uploadLocalFileCallback(uri));
    }

    private static void waitForFile(String filePath) {
        int retires = RETRY_COUNT;
        while (new File(filePath).length() == 0 && retires-- > 0) {
            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Error while waiting for the local file", e);
            }
        }
    }

    private Callback<UploadLocalFileResponse> uploadLocalFileCallback(final Uri uri) {
        return new Callback<UploadLocalFileResponse>() {
            @Override
            public void onResponse(Call<UploadLocalFileResponse> call, Response<UploadLocalFileResponse> response) {
                if (response.isSuccessful()) {
                    onFileUploadSuccess(response.body(), uri);
                } else {
                    handleUploadFileError(uri, getErrorType(response));
                }
            }

            @Override
            public void onFailure(Call<UploadLocalFileResponse> call, Throwable throwable) {
                handleUploadFileError(uri, ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
            }
        };
    }

    private void onFileUploadSuccess(UploadLocalFileResponse response, Uri fileUri) {
        final FPFile fpFile = response.parseToFpFile();
        if (fpFile != null) {
            fpFile.setLocalPath(fileUri.toString());
            ArrayList<FPFile> fpFiles = new ArrayList<>();
            fpFiles.add(fpFile);
            EventBus.getDefault().post(new FpFilesReceivedEvent(fpFiles));
        } else {
            Log.e(LOG_TAG, "onFileUploadSuccess: Failed to retrieve FpFile from the response");
            handleApiError(ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
        }
    }

    /** Exports file to service
     node - destination node
     fileUri - uri to file on device
     filename - new filename given by user
    */
    private void handleActionExportFile(Node node, Uri fileUri, String filename) {
        String fileExtension = FilesUtils.getFileExtension(this, fileUri);
        final String path = FilesUtils.getFilePath(node, filename, fileExtension);
        RequestBody content = FilesUtils.buildRequestBody(this, fileUri);

        FpApiClient.getFpApiClient(this)
            .exportFile(path, FpApiClient.getJsSession(this), content)
            .enqueue(new Callback<FPFile>() {
                @Override
                public void onResponse(Call<FPFile> call, Response<FPFile> response) {
                    if (response.isSuccessful()) {
                        EventBus.getDefault().post(new FileExportedEvent(path, response.body()));
                        Log.d(LOG_TAG, "success");
                    } else {
                        Log.d(LOG_TAG, "failure");
                    }
                }

                @Override
                public void onFailure(Call<FPFile> call, Throwable throwable) {
                    Log.e(LOG_TAG, "failure", throwable);
                }
            });
    }

    private void handleApiError(ApiErrorEvent.ErrorType errorType) {
        EventBus.getDefault().post(new ApiErrorEvent(errorType));
    }

    private void handleUploadFileError(Uri uri, ApiErrorEvent.ErrorType errorType) {
        EventBus.getDefault().post(new UploadFileErrorEvent(uri, errorType));
    }

    public ApiErrorEvent.ErrorType getErrorType(Response error) {
        if (error != null) {
            return ApiErrorEvent.ErrorType.UNAUTHORIZED;
        }
        return ApiErrorEvent.ErrorType.UNKNOWN_ERROR;
    }

    private static class ProgressRequestBody extends RequestBody {

        private static final int BUFFER_SIZE = 4096;

        private File mFile;
        private String mPath;
        private final Listener mListener;

        public ProgressRequestBody(File file, String path, Listener listener) {
            mFile = file;
            mPath = path;
            mListener = listener;
        }

        @Override
        public MediaType contentType() {
            String extension = MimeTypeMap.getFileExtensionFromUrl(mPath);
            String type = "";
            if (extension != null) {
                 type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return MediaType.parse(type);
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            long fileLength = mFile.length();
            byte[] buffer = new byte[BUFFER_SIZE];
            FileInputStream in = new FileInputStream(mFile);
            long uploaded = 0;

            try {
                int read;

                while ((read = in.read(buffer)) != -1 && !Thread.interrupted()) {
                    uploaded += read;
                    sink.write(buffer, 0, read);
                    mListener.onProgress(((float) uploaded) / fileLength);
                }
            } finally {
                in.close();
            }
        }

        interface Listener {

            void onProgress(float progress);

        }

    }
}

