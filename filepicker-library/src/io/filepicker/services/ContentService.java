package io.filepicker.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.greenrobot.event.EventBus;
import io.filepicker.Filepicker;
import io.filepicker.api.FpApiClient;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FileExportedEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GoogleDriveContentEvent;
import io.filepicker.events.GoogleDriveError;
import io.filepicker.events.GoogleDriveUploadProgressEvent;
import io.filepicker.events.GotContentEvent;
import io.filepicker.events.UploadFileErrorEvent;
import io.filepicker.events.UploadProgressEvent;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.GoogleDriveNode;
import io.filepicker.models.Node;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.Constants;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.Utils;
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
    private static final String ACTION_GET_DRIVE_CONTENT = "io.filepicker.services.action.get_drive_content";
    private static final String ACTION_GET_GMAIL_CONTENT = "io.filepicker.services.action.get_gmail_content";
    private static final String ACTION_GET_GPHOTOS_CONTENT = "io.filepicker.services.action.get_gphotos_content";
    public  static final String ACTION_GET_CANCEL_OPERATION = "io.filepicker.services.action.cancel_operation";
    public  static final String SERVICE_TERMINATED = "io.filepicker.services.service_terminated";

    private static final String EXTRA_BACK_PRESSED = "io.filepicker.services.extra.back_pressed";
    private static final String EXTRA_NODE = "io.filepicker.services.extra.node";
    private static final String EXTRA_FILENAME = "io.filepicker.services.extra.filename";

    // Used for upload file action and uri looks like content://<path to local file>
    private static final String EXTRA_FILE_URI = "io.filepicker.services.extra.file_uri";
    private static final int RETRY_COUNT = 10;
    private static final int RETRY_INTERVAL = 2000;

    /*public interface FilepickerListener {
        void onLocalFileUploaded(List<FPFile> files);
    }*/

    public class MessageReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!cancelled){
                cancelled = true;
                if(currentCall!=null)currentCall.cancel();
                cancelAll();
                if(countDownLatch != null) {
                    while (countDownLatch.getCount() > 0){
                        countDownLatch.countDown();
                    }
                }
            }
        }
    }

    //private FilepickerListener filepickerListener;
    private boolean cancelled;
    private Call currentCall;
    private MessageReceiver messageReceiver;
    private CountDownLatch countDownLatch;

    public MessageReceiver getMessageReceiver() {
        if(messageReceiver == null){
          messageReceiver =  new MessageReceiver();
        }
        return messageReceiver;
    }

    public ContentService() {
        super("ContentService");
    }

    //Actions Dispatched from Activity
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

    public static void getGPhotosContent(Context context, Node node, boolean backPressed){
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, ContentService.class)
                        .setAction(ACTION_GET_GPHOTOS_CONTENT);

        if(node  instanceof  GoogleDriveNode) {
            intent.putExtra(EXTRA_NODE, node);
        }
        intent.putExtra(EXTRA_BACK_PRESSED, backPressed);
        context.startService(intent);
    }

    public static void getGMailContent(Context context, Node node, boolean backPressed){
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_GET_GMAIL_CONTENT);
        if(node  instanceof  GoogleDriveNode) {
            intent.putExtra(EXTRA_NODE, node);
        }
        intent.putExtra(EXTRA_BACK_PRESSED, backPressed);
        context.startService(intent);

    }

    public static void getGoogleDriveContent(Context context, Node node, boolean backPressed) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, ContentService.class);
        intent.setAction(ACTION_GET_DRIVE_CONTENT);
        if(node  instanceof  GoogleDriveNode) {
            intent.putExtra(EXTRA_NODE,node);
        }
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
        cancelled = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GET_CANCEL_OPERATION);
        if (intent != null) {
            String action = intent.getAction();
            Node node;
            GoogleDriveNode gNode= null;
            boolean backPressed;

            switch (action) {
                case ACTION_GET_GPHOTOS_CONTENT:
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(getMessageReceiver(),filter);
                    if(intent.hasExtra(EXTRA_NODE))
                        gNode = intent.getParcelableExtra(EXTRA_NODE);
                    backPressed = intent.getBooleanExtra(EXTRA_BACK_PRESSED,false);
                    handleActionGetGPhotosContent(gNode, backPressed);
                    if(cancelled)
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SERVICE_TERMINATED));
                    LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(getMessageReceiver());
                    break;

                case ACTION_GET_GMAIL_CONTENT:
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(getMessageReceiver(),filter);
                    if(intent.hasExtra(EXTRA_NODE))
                        gNode = intent.getParcelableExtra(EXTRA_NODE);
                    backPressed = intent.getBooleanExtra(EXTRA_BACK_PRESSED,false);
                    handleActionGetGMailContent(gNode, backPressed);
                    if(cancelled)
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SERVICE_TERMINATED));
                    LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(getMessageReceiver());
                    break;

                case ACTION_GET_DRIVE_CONTENT:
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(getMessageReceiver(),filter);
                    if(intent.hasExtra(EXTRA_NODE))
                        gNode = intent.getParcelableExtra(EXTRA_NODE);
                    backPressed = intent.getBooleanExtra(EXTRA_BACK_PRESSED,false);
                    handleActionGetDriveContent(gNode, backPressed);
                    if(cancelled)
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SERVICE_TERMINATED));
                    LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(getMessageReceiver());
                    break;

                case ACTION_GET_CONTENT:
                    node = intent.getParcelableExtra(EXTRA_NODE);
                    backPressed = intent.getBooleanExtra(EXTRA_BACK_PRESSED, false);
                    handleActionGetContent(node, backPressed);
                    break;

                case ACTION_UPLOAD_FILE:
                    Uri uri = intent.getParcelableExtra(EXTRA_FILE_URI);
                    handleActionUploadFile(uri);
                    break;

                case ACTION_PICK_FILES:
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(getMessageReceiver(),filter);
                    ArrayList<Node> files = intent.getParcelableArrayListExtra(EXTRA_NODE);
                    handleActionPickFiles(files);
                    if(cancelled)
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SERVICE_TERMINATED));
                    LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(getMessageReceiver());
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

    private void handleActionGetGPhotosContent(GoogleDriveNode gNode, boolean backPressed){
        com.google.api.services.drive.Drive mService;
        Exception mLastError;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, Filepicker.getGPhotosCredential(this))
                .setApplicationName("FileStack")
                .build();
        try {
            SharedPreferences preferences = getSharedPreferences(Filepicker.PREF_PAGINATION,MODE_PRIVATE);
            String pageToken = preferences.getString(Filepicker.PREF_GPHOTOS_PAGE,null);
            boolean loadMore = (pageToken != null && pageToken.length() > 0);

            GoogleDriveContentEvent event = new GoogleDriveContentEvent(getDataFromPhotosApi(mService,gNode),backPressed,loadMore);
            if(!cancelled) EventBus.getDefault().post(event);
        } catch (Exception e) {
            mLastError = e;
            EventBus.getDefault().post(new GoogleDriveError(mLastError));
        }
    }

    private void handleActionGetGMailContent(GoogleDriveNode gNode, boolean backPressed){
        com.google.api.services.gmail.Gmail mService;
        Exception mLastError;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, Filepicker.getGmailCredential(getApplicationContext()))
                .setApplicationName("FileStack")
                .build();
        try {
            GoogleDriveContentEvent event = new GoogleDriveContentEvent(getDataFromGmailApi(mService,gNode),backPressed,false);
            if(!cancelled) EventBus.getDefault().post(event);
        } catch (Exception e) {
            mLastError = e;
            EventBus.getDefault().post(new GoogleDriveError(mLastError));
        }
    }

    private void handleActionGetDriveContent(GoogleDriveNode gNode, boolean backPressed){
        com.google.api.services.drive.Drive mService;
        Exception mLastError;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, Filepicker.getGoogleCredential(this))
                .setApplicationName("FileStack")
                .build();

        try {
            SharedPreferences preferences = getSharedPreferences(Filepicker.PREF_PAGINATION,MODE_PRIVATE);
            String pageToken = preferences.getString(Filepicker.PREF_DRIVE_PAGE,null);
            boolean loadMore = (pageToken != null && pageToken.length() > 0);
            GoogleDriveContentEvent event = new GoogleDriveContentEvent(getDataFromDriveApi(mService,gNode),backPressed,loadMore);
            if(!cancelled) EventBus.getDefault().post(event);
        } catch (Exception e) {
            mLastError = e;
            EventBus.getDefault().post(new GoogleDriveError(mLastError));
        }
    }

    private void handleActionGetContent(Node node, final boolean backPressed) {
        if(!cancelled) {
            (currentCall= FpApiClient.getFpApiClient(this)
                    .getFolder(node.linkPath, "info", FpApiClient.getJsSession(this)))
                    .enqueue(new Callback<Folder>() {
                        @Override
                        public void onResponse(Call<Folder> call, Response<Folder> response) {
                            currentCall = null;
                            if (response.isSuccessful()) {
                                if(!cancelled)EventBus.getDefault().post(new GotContentEvent(response.body(), backPressed));
                            } else {
                                if(!cancelled)handleApiError(getErrorType(response));
                            }
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Call<Folder> call, Throwable throwable) {
                            currentCall = null;
                            if(!cancelled)handleApiError(ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
                            countDownLatch.countDown();
                        }
                    });
        }
    }

    private void handleActionPickFiles(ArrayList<Node> nodes) {
        final ArrayList<FPFile> results = new ArrayList<>();

        try {
            countDownLatch = new CountDownLatch(nodes.size());
            for (Node node : nodes) {
                if(node instanceof GoogleDriveNode){
                    GoogleDriveNode gNode = (GoogleDriveNode)node;
                    java.io.File file = null;
                    EventBus.getDefault().post(new GoogleDriveUploadProgressEvent(gNode,0));
                    if(gNode.driveType.equals(Constants.TYPE_DRIVE) || gNode.driveType.equals(Constants.TYPE_PICASA) ) {
                        if(!cancelled) {
                            downloadDriveFile(gNode);
                        }
                        file = Utils.getSDFile(gNode.displayName+ Constants.getExtension(gNode));
                    }else if(gNode.driveType.equals(Constants.TYPE_GMAIL)){
                        if(!cancelled) {
                            downloadGmailFile(gNode);
                        }
                        file = Utils.getSDFile(gNode.displayName);
                    }

                    if(file.exists()) {
                        RequestBody requestBody = new ProgressRequestBody(file, file.getPath(), (new ProgressRequestBody.Listener() {
                            private GoogleDriveNode gNode;
                            ProgressRequestBody.Listener setNode(GoogleDriveNode pNode) {
                                gNode = pNode;
                                return this;
                            }

                            @Override
                            public void onProgress(float progress) {
                                EventBus.getDefault().post(new GoogleDriveUploadProgressEvent(gNode,Double.valueOf(.5 + progress *.5).floatValue()));
                            }
                        }).setNode(gNode));
                        String remoteName = file.getName();
                        if (!cancelled) {
                            (currentCall = FpApiClient.getFpApiClient(this).uploadFile(
                                    remoteName,
                                    FpApiClient.getJsSession(this),
                                    requestBody
                            )).enqueue((new Callback<UploadLocalFileResponse>() {

                                private File file;
                                Callback<UploadLocalFileResponse> setFile(File pFile) {
                                    file = pFile;
                                    return this;
                                }

                                @Override
                                public void onResponse(Call<UploadLocalFileResponse> call, Response<UploadLocalFileResponse> response) {
                                    currentCall = null;
                                    FPFile fpFile = response.body().parseToFpFile();
                                    if (fpFile != null) {
                                        fpFile.setLocalPath(file.getPath());
                                        results.add(fpFile);

                                    } else {
                                        Log.e(LOG_TAG, "onFileUploadSuccess: Failed to retrieve FpFile from the response");
                                        handleApiError(ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
                                    }
                                    countDownLatch.countDown();
                                }

                                @Override
                                public void onFailure(Call<UploadLocalFileResponse> call, Throwable throwable) {
                                    Log.e(LOG_TAG, "Error", throwable);
                                    currentCall = null;
                                    countDownLatch.countDown();
                                }
                            }).setFile(file));
                        }
                    }

                }else {
                    if(!cancelled) {
                        (currentCall =FpApiClient.getFpApiClient(this)
                                .pickFile(URLDecoder.decode(node.linkPath, "utf-8"), "fpurl", FpApiClient.getJsSession(this)))
                                .enqueue(new Callback<FPFile>() {
                                    @Override
                                    public void onResponse(Call<FPFile> call, Response<FPFile> response) {
                                        currentCall = null;
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
                                        currentCall = null;
                                        countDownLatch.countDown();
                                    }
                                });
                    }
                }
            }
            countDownLatch.await();

            if(!results.isEmpty()) {
                EventBus.getDefault().post(new FpFilesReceivedEvent(results));
            } else {
                Log.e(LOG_TAG, "handleActionPickFiles: Failed to retrieve FpFiles from the response");
                if(!cancelled) {
                    handleApiError(ApiErrorEvent.ErrorType.UNKNOWN_ERROR);
                }
            }


        } catch (Exception syntaxException) {
            handleApiError(ApiErrorEvent.ErrorType.WRONG_RESPONSE);
        }
    }

    /**
     * Uploads a File in local Storage
     * @param uri Locator for the file in local FileSystem
     */
    private void handleActionUploadFile(final Uri uri) {
        ApiErrorEvent.ErrorType errorType = null;
        RequestBody requestBody = null;

        String filePath = FilesUtils.getPath(this, uri);
        if (filePath == null) {
            handleUploadFileError(uri, ApiErrorEvent.ErrorType.INVALID_FILE);
            return;
        }
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

        if(!cancelled) {
            (currentCall =FpApiClient.getFpApiClient(this).uploadFile(
                    Utils.getUploadedFilename(file.getName()),
                    FpApiClient.getJsSession(this),
                    requestBody
            )).enqueue(uploadLocalFileCallback(uri));
        }
    }

    /**
     * Exports file to service
     * @param filename new filename given by user
     * @param node destination node
     * @param fileUri uri to file on device
     */

    private void handleActionExportFile(Node node, Uri fileUri, String filename) {
        String fileExtension = FilesUtils.getFileExtension(this, fileUri);
        final String path = FilesUtils.getFilePath(node, filename, fileExtension);
        RequestBody content = FilesUtils.buildRequestBody(this, fileUri);

        if(!cancelled) {
            (currentCall = FpApiClient.getFpApiClient(this)
                    .exportFile(path, FpApiClient.getJsSession(this), content))
                    .enqueue(new Callback<FPFile>() {
                        @Override
                        public void onResponse(Call<FPFile> call, Response<FPFile> response) {
                            currentCall =  null;
                            if (response.isSuccessful()) {
                                if(!cancelled)EventBus.getDefault().post(new FileExportedEvent(path, response.body()));
                                Log.d(LOG_TAG, "success");
                            } else {
                                Log.d(LOG_TAG, "failure");
                            }
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Call<FPFile> call, Throwable throwable) {
                            Log.e(LOG_TAG, "failure", throwable);
                            countDownLatch.countDown();
                        }
                    });
        }
    }

    private void downloadGmailFile(GoogleDriveNode gNode){
        com.google.api.services.gmail.Gmail mService;
        Exception mLastError;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, Filepicker.getGmailCredential(getApplicationContext()))
                .setApplicationName("FileStack")
                .build();
        String user = "me";
        try {
            Gmail.Users.Messages.Attachments.Get attachmentRequest = mService.users().messages().attachments().
                    get(user, gNode.exportFormat, gNode.driveId);

            MessagePartBody attachPart = attachmentRequest.execute();
            java.io.File file = Utils.getSDFile(gNode.displayName);
            Base64 base64Url = new Base64(true);
            byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
            FileOutputStream fileOutFile = new FileOutputStream(file);
            fileOutFile.write(fileByteArray);
            fileOutFile.close();

        } catch (Exception e) {
            mLastError = e;
            EventBus.getDefault().post(new GoogleDriveError(mLastError));
        }
    }

    private void downloadDriveFile(GoogleDriveNode gNode) throws Exception{
        Drive mService = null;
        Exception mLastError;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        if(gNode.driveType.equals(Constants.TYPE_DRIVE)) {
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, Filepicker.getGoogleCredential(this))
                    .setApplicationName("FileStack")
                    .build();
        }else if(gNode.driveType.equals(Constants.TYPE_PICASA)){
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, Filepicker.getGPhotosCredential(this))
                    .setApplicationName("FileStack")
                    .build();
        }

        try {

            java.io.File file = Utils.getSDFile(gNode.displayName + Constants.getExtension(gNode));
            FileOutputStream output = new FileOutputStream(file);
            if(gNode.mimeType.equals(gNode.exportFormat) || gNode.driveType.equals(Constants.TYPE_PICASA)) {
                Drive.Files.Get downloadRequest = mService.files().get(gNode.driveId);
                downloadRequest.getMediaHttpDownloader().setProgressListener(new DownloadProgressListener().setNode(gNode)).setChunkSize(5*0x100000);
                downloadRequest.executeMediaAndDownloadTo(output);
            }else{
                Drive.Files.Export exportRequest = mService.files().export(gNode.driveId, gNode.exportFormat);
                exportRequest.getMediaHttpDownloader().setProgressListener(new DownloadProgressListener().setNode(gNode)).setChunkSize(5*0x100000);
                exportRequest.executeMediaAndDownloadTo(output);
            }
            output.close();
        } catch (Exception e) {
            mLastError = e;
            EventBus.getDefault().post(new GoogleDriveError(mLastError));
        }
    }

    private ArrayList<GoogleDriveNode> getDataFromGmailApi(Gmail mService, GoogleDriveNode queryNode) throws IOException{
        // Get the labels in the user's account.
        String user = "me", query = "has:attachment in:inbox";
        ArrayList<GoogleDriveNode> gMailNodes = new ArrayList<>();
        GoogleDriveNode gNode;

        if(queryNode == null) {
            ListMessagesResponse response = mService
                    .users()
                    .messages()
                    .list(user)
                    .setQ(query)
                    .setFields("nextPageToken,messages(id,threadId,internalDate)")
                    .execute();
            List<Message> messages = new ArrayList<>();
            while (response.getMessages() != null && !cancelled) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = mService.users().messages().list(user).setQ(query)
                            .setPageToken(pageToken).execute();
                }else {
                    break;
                }
            }
            for(Message m:messages){
                if(cancelled)break;
                gNode = new GoogleDriveNode();
                gNode.driveId = m.getId();
                gNode.displayName = m.getId();
                gNode.thumbExists = false;
                gNode.isDir = true;
                gNode.exportFormat = "";
                gNode.driveType = Constants.TYPE_GMAIL;
                gMailNodes.add(gNode);
            }


        }else{
            if(queryNode.isDir){
                Message message = mService.users().messages().get(user, queryNode.driveId).setFields("payload").execute();
                List<MessagePart> parts = message.getPayload().getParts();
                for (MessagePart part : parts) {
                    if(cancelled)break;
                    if (part.getFilename() != null && part.getFilename().length() > 0) {
                        if(part.getFilename().length() > 0 && !part.getMimeType().equals("text/plain")){
                            gNode = new GoogleDriveNode();
                            gNode.driveId = part.getBody().getAttachmentId();
                            gNode.mimeType = part.getMimeType();
                            gNode.displayName = part.getFilename();
                            gNode.thumbExists = false;
                            gNode.isDir = part.getMimeType().equals("application/vnd.google-apps.folder");
                            gNode.iconLink = "";
                            gNode.thumbnailUrl = "";
                            gNode.exportFormat = queryNode.driveId;
                            gNode.driveType = Constants.TYPE_GMAIL;
                            gMailNodes.add(gNode);
                        }
                    }
                }
            }
        }
        return gMailNodes;
    }

    private ArrayList<GoogleDriveNode> getDataFromPhotosApi(Drive mService, GoogleDriveNode queryNode) throws IOException {
        ArrayList<GoogleDriveNode> driveNodes = new ArrayList<>();
        List<com.google.api.services.drive.model.File> files = null;
        String pageToken,query = "";
        SharedPreferences preferences = getSharedPreferences(Filepicker.PREF_PAGINATION,MODE_PRIVATE);
        pageToken = preferences.getString(Filepicker.PREF_GPHOTOS_PAGE,null);

        if(queryNode !=  null) {
            if (queryNode.isDir) query = "'" + queryNode.driveId + "' in parents and 'me' in owners";
        }else{
            query = "'me' in owners";// and 'me' in owners";
        }

        if(pageToken != null) {
            FileList result = mService.files()
                    .list()
                    .setSpaces("photos")
                    .setOrderBy("createdTime desc")
                    .setQ(query)
                    .setPageToken(pageToken)
                    .setPageSize(50)
                    .setFields("nextPageToken, files(id,parents, name,hasThumbnail,iconLink,mimeType,thumbnailVersion,thumbnailLink)")
                    .execute();
            files = result.getFiles();
            GoogleDriveNode gNode;
            pageToken = result.getNextPageToken();
            preferences.edit().putString(Filepicker.PREF_GPHOTOS_PAGE,pageToken).commit();
            if (files != null) {
                for (com.google.api.services.drive.model.File file : files) {
                    gNode = new GoogleDriveNode();
                    gNode.driveId = file.getId();
                    gNode.displayName = file.getName();
                    gNode.thumbExists = file.getHasThumbnail();
                    gNode.iconLink = file.getIconLink();
                    gNode.mimeType = file.getMimeType();
                    gNode.thumbnailUrl = file.getThumbnailLink();
                    gNode.isDir = file.getMimeType().equals("application/vnd.google-apps.folder");
                    gNode.exportFormat = "";
                    gNode.driveType = Constants.TYPE_PICASA;
                    driveNodes.add(gNode);
                }
            }
        }
        return driveNodes;
    }

    private ArrayList<GoogleDriveNode> getDataFromDriveApi(Drive mService, GoogleDriveNode queryNode) throws IOException {
        ArrayList<GoogleDriveNode> driveNodes = new ArrayList<>();
        List<com.google.api.services.drive.model.File> files;
        String pageToken,query = "";
        SharedPreferences preferences = getSharedPreferences(Filepicker.PREF_PAGINATION,MODE_PRIVATE);
        pageToken = preferences.getString(Filepicker.PREF_DRIVE_PAGE,null);

        if(queryNode !=  null) {
            if (queryNode.isDir) query = "'" + queryNode.driveId + "' in parents and 'me' in owners";
        }else{
            com.google.api.services.drive.model.File rootFile = mService.files()
                    .get("root")
                    .setFields("id")
                    .execute();
            query = "'"+rootFile.getId()+"' in parents ";// and 'me' in owners";
        }

        if(Filepicker.getDriveAbout() == null) {
            About about = mService.about().get().setFields("exportFormats").execute();
            Filepicker.setDriveAbout(about);
        }

        if(pageToken != null) {
            FileList result = mService.files()
                    .list()
                    .setQ(query)
                    .setOrderBy("folder,name")
                    .setPageToken(pageToken)
                    .setPageSize(50)
                    .setFields("nextPageToken, files(id,parents, name,hasThumbnail,iconLink,mimeType,thumbnailVersion,thumbnailLink)")
                    .execute();
            files = result.getFiles();
            GoogleDriveNode gNode;
            pageToken = result.getNextPageToken();
            preferences.edit().putString(Filepicker.PREF_DRIVE_PAGE,pageToken).commit();
            if (files != null) {
                for (com.google.api.services.drive.model.File file : files) {
                    gNode = new GoogleDriveNode();
                    gNode.driveId = file.getId();
                    gNode.displayName = file.getName();
                    gNode.thumbExists = file.getHasThumbnail();
                    gNode.iconLink = file.getIconLink();
                    gNode.mimeType = file.getMimeType();
                    gNode.thumbnailUrl = file.getThumbnailLink();
                    gNode.isDir = file.getMimeType().equals("application/vnd.google-apps.folder");
                    gNode.exportFormat = "";
                    gNode.driveType = Constants.TYPE_DRIVE;
                    driveNodes.add(gNode);
                }
            }
        }
        return driveNodes;
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
                currentCall = null;
                if (response.isSuccessful()) {
                   onFileUploadSuccess(response.body(), uri);
                } else {
                    handleUploadFileError(uri, getErrorType(response));
                }

            }

            @Override
            public void onFailure(Call<UploadLocalFileResponse> call, Throwable throwable) {
                currentCall = null;
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

    //custom listener for download progress
    private class DownloadProgressListener implements MediaHttpDownloaderProgressListener{
        private GoogleDriveNode node;


        public DownloadProgressListener setNode(GoogleDriveNode node){
            this.node = node;
            return this;
        }

        @Override
        public void progressChanged(MediaHttpDownloader downloader) throws IOException {
            switch (downloader.getDownloadState()){

                //Called when file is still downloading
                //ONLY CALLED AFTER A CHUNK HAS DOWNLOADED,SO SET APPROPRIATE CHUNK SIZE
                case MEDIA_IN_PROGRESS:
                    EventBus.getDefault().post(new GoogleDriveUploadProgressEvent(node,Double.valueOf(downloader.getProgress()*.5).floatValue()));
                    break;
                //Called after download is complete
                case MEDIA_COMPLETE:
                    //Add code for download completion
                    break;
            }
        }
    }

    private static class ProgressRequestBody extends RequestBody {

        private static final int BUFFER_SIZE = 4096;

        private File mFile;
        private String mPath;
        private final Listener mListener;

        ProgressRequestBody(File file, String path, Listener listener) {
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
            try{
                int read;
                while ((read = in.read(buffer)) != -1 && !Thread.interrupted()) {
                    uploaded += read;
                    sink.write(buffer, 0, read);
                    mListener.onProgress(((float) uploaded) / fileLength);
                }
            }finally {
                in.close();
            }
        }

        interface Listener {
            void onProgress(float progress);
        }

    }
}

