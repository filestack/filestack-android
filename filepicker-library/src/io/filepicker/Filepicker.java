package io.filepicker;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.gmail.GmailScopes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.filepicker.adapters.NodesAdapter;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FileExportedEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GoogleDriveContentEvent;
import io.filepicker.events.GoogleDriveError;
import io.filepicker.events.GoogleDriveUploadProgressEvent;
import io.filepicker.events.GotContentEvent;
import io.filepicker.events.SignedOutEvent;
import io.filepicker.events.UploadFileErrorEvent;
import io.filepicker.models.DisplayedNode;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.GoogleDriveNode;
import io.filepicker.models.Node;
import io.filepicker.models.Provider;
import io.filepicker.services.ContentService;
import io.filepicker.utils.Constants;
import io.filepicker.utils.LoadMoreEvent;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.SessionUtils;
import io.filepicker.utils.Utils;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class Filepicker extends FragmentActivity implements AuthFragment.Contract,
                                                            NodesFragment.Contract,
                                                            ExportFragment.Contract,
                                                            EasyPermissions.PermissionCallbacks{

    private static final String LOG_TAG = Filepicker.class.getSimpleName();

    private static final String AUTH_FRAGMENT_TAG = "authFragment";

    public static final String NODE_EXTRA = "node";
    public static final String FPFILES_EXTRA = "fpfile";

    // We call this extra 'services' just because users are more used to this name
    private static final String SELECTED_PROVIDERS_EXTRA = "services";
    private static final String MULTIPLE_EXTRA = "multiple";
    private static final String MIMETYPE_EXTRA = "mimetype";
    private static final String LOCATION_EXTRA = "location";
    private static final String PATH_EXTRA = "path";
    private static final String CONTAINER_EXTRA = "container";
    private static final String ACCESS_EXTRA = "access";
    private static final String MAX_FILES_EXTRA = "maxFiles";
    private static final String MAX_SIZE_EXTRA = "maxSize";
    private static final String SHOW_ERROR_TOAST_EXTRA = "showErrorToast";

    // Security extras
    private static final String SECRET_EXTRA = "app_secret";
    private static final String POLICY_CALLS_EXTRA = "policy_calls";
    private static final String POLICY_HANDLE_EXTRA = "policy_handle";
    private static final String POLICY_EXPIRY_EXTRA = "policy_expiry";
    private static final String POLICY_MAX_SIZE_EXTRA = "policy_max_size";
    private static final String POLICY_MIN_SIZE_EXTRA = "policy_min_size";
    private static final String POLICY_PATH_EXTRA = "policy_path";
    private static final String POLICY_CONTAINER_EXTRA = "policy_container";

    private static final String API_KEY_STATE = "api_key_state";
    private static final String IMAGE_URI_STATE = "image_uri_state";
    private static final String LOADING_STATE = "loading_state";
    private static final String DISPLAYED_NODES_LIST_STATE = "nodes_list_state";
    private static final String CURRENT_DISPLAYED_NODE_STATE = "current_displayed_node_state";
    private static final String NODE_CONTENT_STATE = "node_content_state";
    private static final String IS_USER_AUTHORIZED_STATE = "is_user_authorized_state";
    private static final String FOLDER_CLIENT_CODE_STATE = "folder_client_code_state";

    public static final int REQUEST_CODE_GETFILE = 601;
    public static final int REQUEST_CODE_TAKE_PICTURE = 602;
    public static final int REQUEST_CODE_GET_LOCAL_FILE = 603;
    public static final int REQUEST_CODE_EXPORT_FILE = 604;
    public static final int REQUEST_CODE_VIDEO = 605;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1004;
    static final int REQUEST_PERMISSION_CAMERA = 1005;


    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PREF_ACCOUNT_NAME_GMAIL = "accountNameGmail";
    private static final String PREF_ACCOUNT_NAME_GPHOTOS = "accountNameGPhotos";

    public static final String PREF_PAGINATION = "gPagination";
    public static final String PREF_DRIVE_PAGE = "gDrivePage";
    public static final String PREF_GPHOTOS_PAGE = "gPhotosPage";


    private static final String[] SCOPES = { DriveScopes.DRIVE_READONLY , DriveScopes.DRIVE_METADATA_READONLY};
    private static final String[] GMAIL_SCOPES = {GmailScopes.GMAIL_READONLY};
    private static final String[] GPHOTOS_SCOPES = {DriveScopes.DRIVE_PHOTOS_READONLY};



    // Action used by clients to indicate they want to export file
    public static final String ACTION_EXPORT_FILE = "export_file";

    private boolean mIsLoading = false;
    private boolean mIsWaitingForContent = false;
    private boolean mPendingAbort = false;

    private static String apiKey = "";
    private static String appName = "";

    // List which will be traversed while the user goes in the folders tree
    private ArrayList<DisplayedNode> mDisplayedNodesList;
    private DisplayedNode mCurrentDisplayedNode = new DisplayedNode(null,"");

    // True is user is authorized to the current node
    private boolean mIsUserAuthorized;

    // The code of the folder's client returned from API
    private String mFolderClientCode;

    private ArrayList<Node> mProviders;
    private ArrayList<? extends Node> mNodeContentList;

    // Needed for camera request
    private Uri imageUri;

    private View mProgressBar;
    private TextView progress_text;
    private static Uri mFileToExport;
    private HashMap<String,String>progressMap;

    private static FilepickerCallbackHandler sFilepickerCallbackHandler = new FilepickerCallbackHandler();

    static boolean mExport = false;
    private boolean allowMultiple = false,activityBack = false;
    private Node moreNode;

    public static void setKey(String apiKey) {
        if (Filepicker.apiKey.isEmpty()) {
            Filepicker.apiKey = apiKey;
        }
    }

    public static String getApiKey() {
        return apiKey;
    }
    public static void setAppName(String appName) {
        Filepicker.appName = appName;
    }
    public static String getAppName() {
        return appName;
    }
    public static void uploadLocalFile(Uri uri, Context context) {
        ContentService.uploadFile(context, uri);
    }
    public static void uploadLocalFile(Uri uri, Context context, FilepickerCallback filepickerCallback) {
        if (uri == null || context == null) {
            return;
        }
        if (filepickerCallback != null) {
            sFilepickerCallbackHandler.addCallback(uri, filepickerCallback);
        }

        uploadLocalFile(uri, context);
    }

    public static void cancelLocalFileUploading() {
        ContentService.cancelAll();
    }

    public static void unregistedLocalFileUploadCallbacks() {
        sFilepickerCallbackHandler.unregister();
    }


    private static GoogleAccountCredential mCredential = null;
    private static GoogleAccountCredential gMailCredential = null;
    private static GoogleAccountCredential gPhotosCredential = null;

    public static GoogleAccountCredential getGPhotosCredential(Context context){
        if(gPhotosCredential == null){
            gPhotosCredential = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(GPHOTOS_SCOPES))
                    .setBackOff(new ExponentialBackOff());
        }
        return gPhotosCredential;
    }

    private class TerminatedReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("test","Terminated");
            if(mPendingAbort) {
                mPendingAbort = false;
                onBackPressed();
            }
        }
    }
    private TerminatedReceiver terminatedReceiver;
    private TerminatedReceiver getReceiver(){
        if(terminatedReceiver == null){
            terminatedReceiver = new TerminatedReceiver();
        }
        return terminatedReceiver;
    }

    public static GoogleAccountCredential getGmailCredential (Context context){
        if(gMailCredential == null){
            gMailCredential = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(GMAIL_SCOPES))
                    .setBackOff(new ExponentialBackOff());
        }
        return gMailCredential;
    }

    public static GoogleAccountCredential getGoogleCredential(Context context){
        if(mCredential == null){
            // Initialize credentials and service object.
            mCredential = GoogleAccountCredential.usingOAuth2(
                    context.getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
        }
        return mCredential;
    }

    private static About driveAbout = null;
    public static About getDriveAbout() {
        return driveAbout;
    }
    public static void setDriveAbout(About driveAbout) {
        Filepicker.driveAbout = driveAbout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);
        mProgressBar =  findViewById(R.id.fpProgressBar);
        progress_text = (TextView) findViewById(R.id.progress_text);
        initSavedState(savedInstanceState);
        activityBack = false;

        if (mDisplayedNodesList == null) {
            mDisplayedNodesList = new ArrayList<>();
        }

        if (mNodeContentList == null) {
            mNodeContentList = new ArrayList<>();
        }

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
    private void checkStoragePermission(){
        if (!EasyPermissions.hasPermissions(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs access to SD Card Storage ",
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        validateApiKey();
        initOptions();

        // Shows provider's folders and files
        if (mDisplayedNodesList.isEmpty()) {
            // Go straight to the only 1 user-specified provider
            if (mProviders.size() == 1) {
                handleSingleProviderScenario();
            } else {
                setTitle(appName);
                showProvidersList();
            }
        } else {
            setTitle(mCurrentDisplayedNode.node.displayName);

            if (!mIsUserAuthorized && !(mCurrentDisplayedNode.node instanceof GoogleDriveNode) &&
                    !Constants.NATIVE_PROVIDERS.contains(((Provider)(mCurrentDisplayedNode.node)).code)) {
                showAuthFragment();
            } else {
                refreshFragment();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkStoragePermission();

    }

    private void handleSingleProviderScenario() {
        Node providerNode = mProviders.get(0);

        if (providerNode.isCamera()) {
            openCamera();
        } else if (providerNode.isGallery()) {
            openGallery(allowMultiple);
        } else {
            showNextNode(providerNode);
        }
    }

    private void initSavedState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            setKey(savedInstanceState.getString(API_KEY_STATE));

            if (savedInstanceState.getString(IMAGE_URI_STATE) != null) {
                imageUri = Uri.parse(savedInstanceState.getString(IMAGE_URI_STATE));
            }

            mIsLoading = savedInstanceState.getBoolean(LOADING_STATE);
            mDisplayedNodesList =  savedInstanceState.getParcelableArrayList(DISPLAYED_NODES_LIST_STATE);
            mCurrentDisplayedNode = savedInstanceState.getParcelable(CURRENT_DISPLAYED_NODE_STATE);
            mNodeContentList = savedInstanceState.getParcelableArrayList(NODE_CONTENT_STATE);
            mIsUserAuthorized = savedInstanceState.getBoolean(IS_USER_AUTHORIZED_STATE);
            mFolderClientCode = savedInstanceState.getString(FOLDER_CLIENT_CODE_STATE);
        }
    }

    private void initOptions() {
        Intent intent = getIntent();

        PreferencesUtils prefs = PreferencesUtils.newInstance(this);
        // Init Multiple option
        if (intent.hasExtra(MULTIPLE_EXTRA)) {
            allowMultiple = intent.getBooleanExtra(MULTIPLE_EXTRA, false);
            prefs.setMultiple(allowMultiple);
        } else {
            prefs.clearMultiple();
        }

        // Init choosing mimetypes
        if (intent.hasExtra(MIMETYPE_EXTRA)) {
            String[] mimetypes = intent.getStringArrayExtra(MIMETYPE_EXTRA);
            prefs.setMimetypes(mimetypes);
        } else {
            prefs.clearMimetypes();
        }

        // Init location
        String location = intent.getStringExtra(LOCATION_EXTRA);
        if (location != null) {
            prefs.setLocation(location);
        }

        // Init path
        String path = intent.getStringExtra(PATH_EXTRA);
        if (path != null) {
            prefs.setPath(path);
        }

        // Init container
        String container = intent.getStringExtra(CONTAINER_EXTRA);
        if (container != null) {
            prefs.setContainer(container);
        }

        // Init access
        String access = intent.getStringExtra(ACCESS_EXTRA);
        if (access != null) {
            prefs.setAccess(access);
        }

        // Init export
        if (isValidExportRequest()) {
            mExport = true;
            mFileToExport = intent.getData();
        } else {
            mExport = false;
            mFileToExport = null;
        }

        // Init Max Files option
        if (intent.hasExtra(MAX_FILES_EXTRA)) {
            prefs.setMaxFiles(intent.getIntExtra(MAX_FILES_EXTRA, -1));
        } else {
            prefs.clearMaxFiles();
        }

        // Init Max Size option
        if (intent.hasExtra(MAX_SIZE_EXTRA)) {
            prefs.setMaxSize(intent.getIntExtra(MAX_SIZE_EXTRA, -1));
        } else {
            prefs.clearMaxSize();
        }

        // Init providers
        String[] selectedProviders = null;
        if (getIntent().hasExtra(SELECTED_PROVIDERS_EXTRA)) {
            selectedProviders = getIntent().getStringArrayExtra(SELECTED_PROVIDERS_EXTRA);
        }
        mProviders = Utils.getProvidersNodes(this, selectedProviders, mExport);
        boolean showShowErrorToast = intent.getBooleanExtra(SHOW_ERROR_TOAST_EXTRA, true);
        prefs.setShowErrorToast(showShowErrorToast);
        prefs.setSecret(intent.getStringExtra(SECRET_EXTRA));
        prefs.setPolicyCalls(intent.getStringArrayExtra(POLICY_CALLS_EXTRA));
        prefs.setPolicyHandle(intent.getStringExtra(POLICY_HANDLE_EXTRA));
        prefs.setPolicyExpiry(intent.getIntExtra(POLICY_EXPIRY_EXTRA, 0));
        prefs.setPolicyMaxSize(intent.getIntExtra(POLICY_MAX_SIZE_EXTRA, 0));
        prefs.setPolicyMinSize(intent.getIntExtra(POLICY_MIN_SIZE_EXTRA, 0));
        prefs.setPolicyPath(intent.getStringExtra(POLICY_PATH_EXTRA));
        prefs.setPolicyContainer(intent.getStringExtra(POLICY_CONTAINER_EXTRA));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save api key in case the activity was destroyed
        outState.putString(API_KEY_STATE, getApiKey());
        outState.putBoolean(LOADING_STATE, mIsLoading);
        outState.putParcelableArrayList(DISPLAYED_NODES_LIST_STATE, mDisplayedNodesList);
        outState.putParcelable(CURRENT_DISPLAYED_NODE_STATE, mCurrentDisplayedNode);
        outState.putParcelableArrayList(NODE_CONTENT_STATE, mNodeContentList);
        outState.putString(FOLDER_CLIENT_CODE_STATE, mFolderClientCode);
        outState.putBoolean(IS_USER_AUTHORIZED_STATE, mIsUserAuthorized);


        if (imageUri != null) {
            outState.putString(IMAGE_URI_STATE, imageUri.toString());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ContentService.SERVICE_TERMINATED);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(getReceiver(),filter);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(getReceiver());
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // In case content is being loaded, we are not waiting for it
        if(mIsWaitingForContent){
            mIsWaitingForContent = false;
            mPendingAbort = true;
            Intent intent =  new Intent(ContentService.ACTION_GET_CANCEL_OPERATION);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            progress_text.setText(Constants.TERMINATING);
            return;
        }
        if(mPendingAbort)return;
        hideLoading();
        if (mDisplayedNodesList != null && !mDisplayedNodesList.isEmpty()) {
            if(mDisplayedNodesList.get(mDisplayedNodesList.size() - 1).node != null) {
                removeLastNode();
            }else{
                mDisplayedNodesList.remove(mDisplayedNodesList.size() - 1);
            }

            if (!mDisplayedNodesList.isEmpty()) {
                mCurrentDisplayedNode = mDisplayedNodesList.get(mDisplayedNodesList.size() - 1);
                setTitle(mCurrentDisplayedNode.node.displayName);

                Log.d(LOG_TAG, "Get cached data from " + mCurrentDisplayedNode.node.linkPath);
                java.io.File currentFile = Utils.getCacheFile(this, mCurrentDisplayedNode.node.deslashedPath());

                if (currentFile.exists()) {
                    showCachedNode(currentFile);
                } else {
                    refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, true);
                }
            } else if (mProviders.size() == 1) {
                mCurrentDisplayedNode = new DisplayedNode(null,"");
                super.onBackPressed();
            } else {
                mCurrentDisplayedNode = new DisplayedNode(null,"");
                setTitle(getAppName());
                showProvidersList();
            }

        } else {
            mCurrentDisplayedNode = new DisplayedNode(null,"");
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        clearCachedFiles();
        super.onDestroy();
    }

    public void getContent(Node node, boolean backPressed) {
        mIsWaitingForContent = true;
        showLoading();
        mPendingAbort = false;
        ContentService.getContent(getApplicationContext(), node, backPressed);
    }

    private void showFolderContent(Folder folder) {
        mCurrentDisplayedNode.viewType = folder.view;
        mNodeContentList = new ArrayList<>(Arrays.asList(folder.nodes));
        new CacheGotItemsTask(this, mCurrentDisplayedNode.node).execute(mNodeContentList);
        refreshFragment();
    }

    private void addFolderContent(ArrayList<GoogleDriveNode>gNodes){
        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler_list);
        mIsWaitingForContent = false;
        if(recycler != null){
            NodesAdapter adapter = (NodesAdapter) recycler.getAdapter();
            if(adapter != null){
                int startP = adapter.getNodes().size();
                for (GoogleDriveNode n: gNodes){
                    adapter.getNodes().add(n);
                }
                adapter.notifyItemRangeInserted(startP,gNodes.size());
                new CacheGotItemsTask(this, mCurrentDisplayedNode.node).execute(adapter.getNodes());
            }
        }
    }

    private void showDriveFolderContent(ArrayList<GoogleDriveNode> gNodes,boolean backPressed) {
        mCurrentDisplayedNode.viewType = Constants.LIST_VIEW;
        if(mCurrentDisplayedNode.node instanceof Provider){
            if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_PICASA)){
                mCurrentDisplayedNode.viewType = Constants.THUMBNAILS_VIEW;
            }
        }

        mNodeContentList = gNodes;
        // Cache items
        new CacheGotItemsTask(this, mCurrentDisplayedNode.node).execute(mNodeContentList);
        refreshFragment();
    }

    private void refreshFragment() {
        getFragmentManager().beginTransaction()
                .replace(R.id.picker_content, getContentFragment())
                .commit();
    }

    private Fragment getContentFragment() {
        if (mExport) {
            return ExportFragment.newInstance(mCurrentDisplayedNode.node, mNodeContentList, mCurrentDisplayedNode.viewType);
        } else {
            return NodesFragment.newInstance(mCurrentDisplayedNode.node, mNodeContentList, mCurrentDisplayedNode.viewType);
        }
    }

    private void showProvidersList() {
        getFragmentManager().beginTransaction()
                .replace(R.id.picker_content, getProvidersFragment())
                .commit();
    }

    private Fragment getProvidersFragment() {
        if (mExport) {
            return ExportFragment.newInstance(null, mProviders, Constants.LIST_VIEW);
        } else {
            return NodesFragment.newInstance(null, mProviders, Constants.LIST_VIEW);
        }
    }

    private void showAuthFragment() {
        final Context context = getApplicationContext();
        if (context != null) {
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,
                        "Connecting to " + mCurrentDisplayedNode.node.displayName + " ...",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.picker_content, AuthFragment.newInstance(mFolderClientCode), AUTH_FRAGMENT_TAG)
                .commit();
    }

    private void  showMoreProgress(){
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                View v = findViewById(R.id.progress_more);
                if(v!= null){
                    v.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void hideMoreProgress(){
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                View v = findViewById(R.id.progress_more);
                if(v!= null){
                v.setVisibility(View.GONE);
                }

            }
        });
    }

    @SuppressWarnings("unused")
    public void onEvent(LoadMoreEvent event){
        if(mCurrentDisplayedNode.node instanceof Provider){
            if(Constants.NATIVE_PROVIDERS.contains(((Provider)(mCurrentDisplayedNode.node)).code)){
                SharedPreferences preferences = getSharedPreferences(PREF_PAGINATION,MODE_PRIVATE);

                if(((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_DRIVE)) {
                    String pageToken = preferences.getString(PREF_DRIVE_PAGE,null);
                    if(pageToken != null) {
                        moreNode = mCurrentDisplayedNode.node;
                        getGoogleDriveContent(mCurrentDisplayedNode.node, false,true);
                    }
                    return;
                }else if(((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_PICASA)){
                    String pageToken = preferences.getString(PREF_GPHOTOS_PAGE,null);
                    if(pageToken != null) {
                        moreNode = mCurrentDisplayedNode.node;
                        getPicasaContent(mCurrentDisplayedNode.node, false,true);
                    }
                    return;
                }

            }
        }

        if(mCurrentDisplayedNode.node instanceof GoogleDriveNode){
            SharedPreferences preferences = getSharedPreferences(PREF_PAGINATION,MODE_PRIVATE);
            if(((GoogleDriveNode)mCurrentDisplayedNode.node).driveType.equals(Constants.TYPE_DRIVE)) {
                String pageToken = preferences.getString(PREF_DRIVE_PAGE,null);
                if(pageToken != null) {
                    moreNode = mCurrentDisplayedNode.node;
                    getGoogleDriveContent(mCurrentDisplayedNode.node, false,true);
                }
            }
        }

    }


    @SuppressWarnings("unused")
    public void onEvent(GoogleDriveUploadProgressEvent event){
        //TODO: HANDLE PROGRESS
        progressMap.put(event.getNode().displayName,
                    event.getNode().displayName + "  " + Math.round(event.getProgress()*100) + "%\n");

        String progress = "Uploading: \n";

        for (String ns:progressMap.values()){
            progress += ns;
        }

        if(!progress_text.getText().toString().equals(Constants.TERMINATING)) {
            new Handler(getMainLooper()).post((new Runnable() {
                private String message;

                public Runnable setEvent(String message) {
                    this.message = message;
                    return this;
                }

                @Override
                public void run() {
                    progress_text.setText(message);
                }
            }).setEvent(progress));
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(GotContentEvent event) {
        if (!mIsWaitingForContent) {
            return;
        }
        mIsWaitingForContent = false;
        hideLoading();
        hideMoreProgress();
        Folder folder = event.folder;
        mIsUserAuthorized = folder.auth;
        mFolderClientCode = event.folder.client;
        if (mIsUserAuthorized) {
            showFolderContent(folder);
        } else {
            showAuthFragment();
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(FpFilesReceivedEvent event) {
        if(!mIsWaitingForContent) {
            return;
        }
        hideLoading();
        mIsWaitingForContent = false;
        ArrayList<FPFile> fpFiles = event.fpFiles;
        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra(FPFILES_EXTRA, fpFiles);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @SuppressWarnings("unused")
    public void onEvent(ApiErrorEvent event) {
        PreferencesUtils prefs = PreferencesUtils.newInstance(this);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ContentService.SERVICE_TERMINATED));
        if (prefs.shouldShowErrorToast()) {
            showErrorMessage(event.error);
        }

        if (event instanceof UploadFileErrorEvent) {
            Intent data = new Intent();
            data.setData(((UploadFileErrorEvent)event).getUri());
            setResult(RESULT_CANCELED, data);
        }
        finish();
    }

    private void showErrorMessage(ApiErrorEvent.ErrorType errorType) {
        int errorMessage = R.string.error_unexpected;
        switch (errorType) {
            case NETWORK:
                errorMessage = R.string.error_connection;
                break;
            case UNAUTHORIZED:
                errorMessage = R.string.error_authorization;
                break;
            case INVALID_FILE:
                errorMessage = R.string.error_invalid_file;
                break;
        }

        Utils.showQuickToast(this, errorMessage);
    }


    @SuppressWarnings("unused")
    public void onEvent(SignedOutEvent event) {
        clearSession(this);
        showProvidersList();
    }

    @SuppressWarnings("unused")
    public void onEvent(FileExportedEvent event) {
        if (!mIsWaitingForContent){
            return;
        }
        hideLoading();
        mIsWaitingForContent = false;
        String message = "File " + event.fpFile.getFilename() + " was exported to " + event.path.split("/")[0];
        Utils.showQuickToast(this, message);
        Intent resultIntent = new Intent();
        ArrayList<FPFile> result = new ArrayList<>();
        result.add(event.fpFile);
        resultIntent.putParcelableArrayListExtra(FPFILES_EXTRA, result);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    @SuppressWarnings("unsused")
    public void onEvent(GoogleDriveContentEvent driveContent){
        if (!mIsWaitingForContent) {
            return;
        }
        hideLoading();
        mIsWaitingForContent = false;
        hideMoreProgress();
        if(driveContent.isLoadMore()){
            if(mCurrentDisplayedNode.node == moreNode)
                moreNode = null;
                addFolderContent(driveContent.getGoogleNodes());
        }else {
            showDriveFolderContent(driveContent.getGoogleNodes(), driveContent.isBackPresed());
        }
    }


    @SuppressWarnings("unused")
    public void onEvent(final GoogleDriveError driveError){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideLoading();
                if (driveError.getException() != null) {
                    if (driveError.getException() instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) driveError.getException())
                                        .getConnectionStatusCode());
                    } else if (driveError.getException() instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) driveError.getException()).getIntent(), Filepicker.REQUEST_AUTHORIZATION);
                    } else {
                        Toast.makeText(getApplicationContext(),"The following error occurred:\n"
                                + driveError.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),R.string.error_request_canceled,Toast.LENGTH_SHORT).show();
                    finish();
                }

            }
        });
    }

    public void proceedAfterAuth() {
        // Try getting content again
        getContent(mCurrentDisplayedNode.node, false);
    }

    private void validateApiKey() {
        if (apiKey.isEmpty() || !apiKey.startsWith("A") || !apiKey.endsWith("z") || apiKey.length() != 22) {
            Toast.makeText(this, R.string.apikey_missing, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void pickFiles(ArrayList<Node> pickedFiles) {
        showLoading();
        progressMap = new HashMap<>();
        mIsWaitingForContent = true;
        mDisplayedNodesList.add(new DisplayedNode(null,""));
        mPendingAbort = false;
        ContentService.pickFiles(getApplicationContext(), pickedFiles);
    }

    // Removes currently last node's cached file and the node itself from the list
    private void removeLastNode() {
        java.io.File file = Utils.getCacheFile(this, mCurrentDisplayedNode.node.deslashedPath());
        if (file.exists()) {
            file.delete();
        }

        mDisplayedNodesList.remove(mCurrentDisplayedNode);
    }

    @Override
    public void showNextNode(Node newNode) {
        mCurrentDisplayedNode = new DisplayedNode(newNode, Constants.LIST_VIEW); // Default view is listview
        mDisplayedNodesList.add(mCurrentDisplayedNode);
        refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, false);
    }

    private void refreshCurrentlyDisplayedNode(DisplayedNode displayedNode, boolean isBackPressed) {
        setTitle(displayedNode.node.displayName);
        if(displayedNode.node instanceof Provider){
            if(Constants.NATIVE_PROVIDERS.contains(((Provider)(displayedNode.node)).code)){
                SharedPreferences preferences = getSharedPreferences(PREF_PAGINATION,MODE_PRIVATE);

                if(((Provider)(displayedNode.node)).code.equals(Constants.TYPE_DRIVE)) {
                    preferences.edit().putString(PREF_DRIVE_PAGE,"").commit();
                    getGoogleDriveContent(displayedNode.node, isBackPressed,false);
                    return;
                }else if(((Provider)(displayedNode.node)).code.equals(Constants.TYPE_GMAIL)){
                    getGmailContent(displayedNode.node,isBackPressed);
                    return;
                }else if(((Provider)(displayedNode.node)).code.equals(Constants.TYPE_PICASA)){
                    preferences.edit().putString(PREF_GPHOTOS_PAGE,"").commit();
                    getPicasaContent(displayedNode.node,isBackPressed,false);
                    return;
                }

            }
        }

        if(displayedNode.node instanceof GoogleDriveNode){
            SharedPreferences preferences = getSharedPreferences(PREF_PAGINATION,MODE_PRIVATE);
            if(((GoogleDriveNode)displayedNode.node).driveType.equals(Constants.TYPE_DRIVE)) {
                preferences.edit().putString(PREF_DRIVE_PAGE,"").commit();
                getGoogleDriveContent(displayedNode.node, isBackPressed,false);
            }else if(((GoogleDriveNode)displayedNode.node).driveType.equals(Constants.TYPE_GMAIL)){
                getGmailContent(displayedNode.node,isBackPressed);
            }
            return;
        }
        getContent(displayedNode.node, isBackPressed);
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getPicasaContent(Node node, boolean backPressed, boolean isMore){
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (getGPhotosCredential(this).getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(Filepicker.this,R.string.error_no_connection_available,Toast.LENGTH_SHORT).show();
        } else {
            mIsWaitingForContent = true;
            mPendingAbort = false;
            if(isMore)showMoreProgress(); else showLoading();
            ContentService.getGPhotosContent(getApplicationContext(),node,backPressed);
        }
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getGmailContent(Node node, boolean backPressed) {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (getGmailCredential(this).getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(Filepicker.this,R.string.error_no_connection_available,Toast.LENGTH_SHORT).show();
        } else {
            mIsWaitingForContent = true;
            mPendingAbort = false;
            showLoading();
            ContentService.getGMailContent(getApplicationContext(),node,backPressed);

        }
    }


    private void getGoogleDriveContent(Node node, boolean backPressed,boolean isMore) {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (getGoogleCredential(this).getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(Filepicker.this,R.string.error_no_connection_available,Toast.LENGTH_SHORT).show();
        } else {
            mIsWaitingForContent = true;
            mPendingAbort = false;
            if(isMore)showMoreProgress(); else showLoading();
            ContentService.getGoogleDriveContent(getApplicationContext(),node,backPressed);
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        hideLoading(); hideMoreProgress();
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0) {
                if (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
            }
        }
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
        if(!EasyPermissions.hasPermissions(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(getApplicationContext(),
                    R.string.error_permission_denied,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = null;

            if(((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_DRIVE)){
                accountName = getPreferences(Context.MODE_PRIVATE)
                        .getString(PREF_ACCOUNT_NAME, null);
            }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_GMAIL)){
                accountName = getPreferences(Context.MODE_PRIVATE)
                        .getString(PREF_ACCOUNT_NAME_GMAIL, null);
            }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_PICASA)){
                accountName = getPreferences(Context.MODE_PRIVATE)
                        .getString(PREF_ACCOUNT_NAME_GPHOTOS,null);
            }

            if (accountName != null) {
                if(((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_DRIVE)){
                    Filepicker.getGoogleCredential(getApplicationContext()).setSelectedAccountName(accountName);
                }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_GMAIL)){
                    Filepicker.getGmailCredential(getApplicationContext()).setSelectedAccountName(accountName);
                }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_PICASA)){
                    Filepicker.getGPhotosCredential(getApplicationContext()).setSelectedAccountName(accountName);
                }
                refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, false);

            } else {
                // Start a dialog from which the user can choose an account
                if(((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_DRIVE)){
                    startActivityForResult(
                            Filepicker.getGoogleCredential(getApplicationContext()).newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_GMAIL)){
                    startActivityForResult(
                            Filepicker.getGmailCredential(getApplicationContext()).newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_PICASA)){
                    startActivityForResult(
                            Filepicker.getGPhotosCredential(getApplicationContext()).newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                }

            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.message_contact_permission_needed),
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                Filepicker.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }


    private void setTitle(String title) {
        if (getActionBar() != null) {
            getActionBar().setTitle(title);
        }
    }

    @Override
    public void exportFile(String filename) {
        showLoading();
        mIsWaitingForContent = true;
        mPendingAbort = false;
        mDisplayedNodesList.add(new DisplayedNode(null,""));
        ContentService.exportFile(getApplicationContext(), mCurrentDisplayedNode.node, mFileToExport, filename);
    }

    @Override
    public Uri getFileToExport() {
        return mFileToExport;
    }

    @Override
    public void logoutUser() {
        Filepicker.clearSession(this);
    }

    @Override
    public void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[] {Manifest.permission.CAMERA},
                    REQUEST_PERMISSION_CAMERA);
            return;
        }
        if(!activityBack) {
            if (isOnlyVideoCamera()) {
                recordVideo();
            } else {
                takePhoto();
            }
        }
    }

    private void recordVideo() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_CODE_VIDEO);
        }
    }

    private void takePhoto() {
        setCameraImageUri();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        try {
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        } catch(ActivityNotFoundException e) {
            Utils.showQuickToast(this, R.string.camera_not_found);
        }
    }

    @Override
    public void openGallery(boolean allowMultiple) {
        if(!activityBack) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(isOnlyVideoCamera() ? Constants.MIMETYPE_VIDEO : Constants.MIMETYPE_IMAGE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
            }
            startActivityForResult(intent, REQUEST_CODE_GET_LOCAL_FILE);
        }
    }

    private void uploadLocalFile(Uri uri) {
        showLoading();

        uploadLocalFile(uri, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // For Camera and Gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                activityBack = true;
                if (resultCode != RESULT_OK) {
                    hideLoading(); hideMoreProgress();
                    Toast.makeText(Filepicker.this,
                            R.string.error_no_google_services,
                            Toast.LENGTH_SHORT).show();
                } else {
                    refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, false);
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                activityBack = true;
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();

                        if(((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_DRIVE)) {
                            editor.putString(PREF_ACCOUNT_NAME, accountName);
                            Filepicker.getGoogleCredential(getApplicationContext()).setSelectedAccountName(accountName);
                        }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_GMAIL)) {
                            editor.putString(PREF_ACCOUNT_NAME_GMAIL, accountName);
                            Filepicker.getGmailCredential(getApplicationContext()).setSelectedAccountName(accountName);
                        }else if (((Provider)(mCurrentDisplayedNode.node)).code.equals(Constants.TYPE_PICASA)) {
                            editor.putString(PREF_ACCOUNT_NAME_GPHOTOS, accountName);
                            Filepicker.getGPhotosCredential(getApplicationContext()).setSelectedAccountName(accountName);
                        }
                        editor.apply();
                        refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, false);
                    }
                }else{hideLoading(); hideMoreProgress();}
                break;
            case REQUEST_AUTHORIZATION:
                activityBack = true;
                if (resultCode == RESULT_OK) {
                    refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, false);
                }else{hideLoading(); hideMoreProgress();}
                break;

            case REQUEST_CODE_GETFILE:
                activityBack = true;
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;
            case REQUEST_CODE_EXPORT_FILE:
                activityBack = true;
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;
            case REQUEST_CODE_GET_LOCAL_FILE:
            case REQUEST_CODE_VIDEO:
                activityBack = true;
                if (resultCode == RESULT_OK) {
                    Utils.showQuickToast(this, R.string.uploading_image);
                    showLoading();
                    if (data.getData() != null) // Single file returned
                        uploadLocalFile(data.getData());
                    else { // Multiple files returned, use getClipData()
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++)
                            uploadLocalFile(clipData.getItemAt(i).getUri());
                    }
                }
                break;
            case REQUEST_CODE_TAKE_PICTURE:
                activityBack = true;
                if (resultCode == RESULT_OK) {
                    Utils.showQuickToast(this, R.string.uploading_image);
                    uploadLocalFile(imageUri);
                }
                break;
        }
    }

    private void showLoading() {
        mIsLoading = true;
        mIsWaitingForContent = true;
        updateLoadingView();
    }

    private void hideLoading() {
        mIsLoading = false;
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                updateLoadingView();
            }
        });
    }

    private void updateLoadingView() {
        mProgressBar.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);
        progress_text.setText("");
    }

    private void setCameraImageUri() {
        String fileName = "" + System.currentTimeMillis() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image captured by camera");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    }

    // Checks if intent has action ACTION_EXPORT_FILE and data
    private boolean isValidExportRequest() {
        Intent intent = getIntent();

        return intent.getAction() != null &&
                intent.getAction().equals(ACTION_EXPORT_FILE) &&
                intent.getData() != null;
    }

    private void showCachedNode(java.io.File nodeCachedData) {
        new AsyncTask<java.io.File, Void, Void>() {
            @Override
            protected Void doInBackground(java.io.File... params) {
                java.io.File nodeCachedData = params[0];
                try {
                    Log.d(LOG_TAG, "Reading from file " + nodeCachedData.getAbsolutePath());
                    BufferedReader br = new BufferedReader(new FileReader(nodeCachedData));
                    String line;
                    while ((line = br.readLine()) != null) {

                        if (nodeCachedData.getName().contains("drive_") ||
                                nodeCachedData.getName().toUpperCase().contains("GOOGLEDRIVE") ||
                                nodeCachedData.getName().toUpperCase().contains("GMAIL")||
                                nodeCachedData.getName().toUpperCase().contains("GOOGLEPHOTOS")){
                            mNodeContentList = new ArrayList<>(Arrays.asList(new Gson().fromJson(line, GoogleDriveNode[].class)));
                        }else{
                            mNodeContentList = new ArrayList<>(Arrays.asList(new Gson().fromJson(line, Node[].class)));
                        }

                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void param) {
                refreshFragment();
            }
        }.execute(nodeCachedData);
    }

    private void clearCachedFiles() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Utils.clearCachedFiles(Filepicker.this);
                return null;
            }
        }.execute();
    }
    private static class CacheGotItemsTask extends AsyncTask<ArrayList<? extends Node>, Void, Void> {

        private final Context mContext;
        private final Node mLastNode;

        public CacheGotItemsTask(Context context, Node lastNode) {
            this.mContext = context;
            this.mLastNode = lastNode;
        }

        @Override
        protected Void doInBackground(ArrayList<? extends Node>... params) {
            final List<? extends Node> nodes = params[0];
            if (nodes != null && !nodes.isEmpty() && mLastNode != null) {
                Gson gson = new Gson();
                JsonElement jsonElement = new JsonParser().parse(gson.toJson(nodes));
                String result = gson.toJson(jsonElement);
                java.io.File file = Utils.getCacheFile(mContext, mLastNode.deslashedPath());
                try {
                    Log.d(LOG_TAG, "Writing to file " + file.getAbsolutePath());
                    FileOutputStream output = new FileOutputStream(file);
                    output.write(result.getBytes());
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static void clearSession(Context context) {
        SessionUtils.clearSessionCookies(context);
    }

    private boolean isOnlyVideoCamera() {
        PreferencesUtils prefs = PreferencesUtils.newInstance(this);
        return prefs.isMimetypeSet("video") && !prefs.isMimetypeSet("image");
    }


}
