package io.filepicker;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import de.greenrobot.event.EventBus;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FileExportedEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GotContentEvent;
import io.filepicker.events.SignedOutEvent;
import io.filepicker.models.DisplayedNode;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.Node;
import io.filepicker.services.ContentService;
import io.filepicker.utils.Constants;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.SessionUtils;
import io.filepicker.utils.Utils;


public class Filepicker extends FragmentActivity
        implements AuthFragment.Contract, NodesFragment.Contract, ExportFragment.Contract {

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

    private static final String API_KEY_STATE = "api_key_state";
    private static final String IMAGE_URI_STATE = "image_uri_state";
    private static final String LOADING_STATE = "loading_state";
    private static final String DISPLAYED_NODES_LIST_STATE = "nodes_list_state";
    private static final String CURRENT_DISPLAYED_NODE_STATE = "current_displayed_node_state";
    private static final String NODE_CONTENT_STATE = "node_content_state";
    private static final String IS_USER_AUTHORIZED_STATE = "is_user_authorized_state";
    private static final String FOLDER_CLIENT_CODE_STATE = "folder_client_code_state";

    private boolean mIsLoading = false;
    private boolean mIsWaitingForContent = false;

    private static String API_KEY = "";
    private static String APP_NAME = "";

    public static final int REQUEST_CODE_GETFILE = 601;
    public static final int REQUEST_CODE_CAMERA = 602;
    public static final int REQUEST_CODE_GET_LOCAL_FILE = 603;
    public static final int REQUEST_CODE_EXPORT_FILE = 604;

    // Action used by clients to indicate they want to export file
    public static final String ACTION_EXPORT_FILE = "export_file";

    // List which will be traversed while the user goes in the folders tree
    private ArrayList<DisplayedNode> mDisplayedNodesList;
    private DisplayedNode mCurrentDisplayedNode;

    // True is user is authorized to the current node
    private boolean mIsUserAuthorized;

    // The code of the folder's client returned from API
    private String mFolderClientCode;

    private ArrayList<Node> mNodeContentList;

    // Needed for camera request
    private Uri imageUri;

    private ProgressBar mProgressBar;
    private static Uri mFileToExport;

    public static void setKey(String apiKey) {
        if(API_KEY.isEmpty()) {
            API_KEY = apiKey;
        }
    }

    static boolean mExport = false;

    public static String getApiKey() {
        return API_KEY;
    }

    public static void setAppName(String appName) {
        APP_NAME = appName;
    }

    public static String getAppName() {
        return APP_NAME;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);

        mProgressBar = (ProgressBar) findViewById(R.id.fpProgressBar);

        initSavedState(savedInstanceState);

        if(mDisplayedNodesList == null) {
            mDisplayedNodesList = new ArrayList<>();
        }

        if(mNodeContentList == null) {
            mNodeContentList = new ArrayList<>();
        }

        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        validateApiKey();
        initOptions();

        // Shows provider's folders and files
        if(mDisplayedNodesList.isEmpty()) {
            setTitle(APP_NAME);
            showProvidersList(false);
        } else {
            setTitle(mCurrentDisplayedNode.node.displayName);

            if(!mIsUserAuthorized) {
                showAuthFragment();
            } else {
                refreshFragment(false);
            }
        }
    }

    private void initSavedState(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            setKey(savedInstanceState.getString(API_KEY_STATE));

            if(savedInstanceState.getString(IMAGE_URI_STATE) != null)
                imageUri = Uri.parse(savedInstanceState.getString(IMAGE_URI_STATE));

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
        if (intent.hasExtra(MULTIPLE_EXTRA))
            prefs.setMultiple(intent.getBooleanExtra(MULTIPLE_EXTRA, false));

        // Init choosing mimetypes
        if (intent.hasExtra(MIMETYPE_EXTRA)){
            String[] mimetypes = intent.getStringArrayExtra(MIMETYPE_EXTRA);
            prefs.setMimetypes(mimetypes);
        }

        // Init location
        String location = intent.getStringExtra(LOCATION_EXTRA);
        if(location != null)
            prefs.setLocation(location);

        // Init path
        String path = intent.getStringExtra(PATH_EXTRA);
        if(path != null)
            prefs.setPath(path);

        // Init container
        String container = intent.getStringExtra(CONTAINER_EXTRA);
        if(container != null)
            prefs.setContainer(container);

        // Init access
        String access = intent.getStringExtra(ACCESS_EXTRA);
        if(access != null)
            prefs.setAccess(access);

        // Init export
        if (isValidExportRequest()) {
            mExport = true;
            mFileToExport = intent.getData();
        } else {
            mExport = false;
            mFileToExport = null;
        }
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

        if(imageUri != null)
            outState.putString(IMAGE_URI_STATE, imageUri.toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // In case content is being loaded, we are not waiting for it
        mIsWaitingForContent = false;

        if(mDisplayedNodesList != null && mDisplayedNodesList.size() > 0) {
            removeLastNode();

            if(mDisplayedNodesList.size() > 0) {
                mCurrentDisplayedNode = mDisplayedNodesList.get(mDisplayedNodesList.size()-1);
                setTitle(mCurrentDisplayedNode.node.displayName);

                Log.d(LOG_TAG, "Get cached data from " + mCurrentDisplayedNode.node.linkPath);
                File currentFile = Utils.getCacheFile(this, mCurrentDisplayedNode.node.deslashedPath());

                if(currentFile.exists()) {
                    showCachedNode(currentFile);
                } else {
                    refreshCurrentlyDisplayedNode(mCurrentDisplayedNode, true);
                }
            } else {
                setTitle(getAppName());
                showProvidersList(true);
            }

        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.right_slide_out_back,
                    R.anim.right_slide_in_back);
        }
    }

    @Override
    protected void onDestroy() {
        clearCachedFiles();
        super.onStop();
    }

    public void getContent(Node node, boolean backPressed) {
        mIsWaitingForContent = true;
        showLoading();

        ContentService.getContent(this, node, backPressed);
    }

    private void showFolderContent(Folder folder, boolean backPressed) {
        mCurrentDisplayedNode.viewType = folder.view;

        mNodeContentList = new ArrayList<>(Arrays.asList(folder.nodes));

        // Cache items
        new CacheGotItemsTask(this, mCurrentDisplayedNode.node).execute(mNodeContentList);

        refreshFragment(backPressed);
    }

    private void refreshFragment(boolean backPressed) {
        hideLoading();

        Fragment contentFragment = getContentFragment();

        // TODO change so only animation are in if/else
        if(backPressed) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.right_slide_out_back, R.anim.right_slide_in_back)
                    .replace(android.R.id.content, contentFragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.right_slide_in, R.anim.right_slide_out)
                    .replace(android.R.id.content, contentFragment)
                    .commit();
        }
    }

    private Fragment getContentFragment() {
        Fragment contentFragment;

        if(mExport) {
            contentFragment = ExportFragment.newInstance(mCurrentDisplayedNode.node,
                    mNodeContentList, mCurrentDisplayedNode.viewType);
        } else {
            contentFragment = NodesFragment.newInstance(mCurrentDisplayedNode.node,
                    mNodeContentList, mCurrentDisplayedNode.viewType);
        }

        return contentFragment;
    }

    private void showProvidersList(boolean backPressed) {
        if(backPressed) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.right_slide_out_back, R.anim.right_slide_in_back)
                    .replace(android.R.id.content, getProvidersFragment())
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.right_slide_in, R.anim.right_slide_out)
                    .replace(android.R.id.content, getProvidersFragment())
                    .commit();
        }
    }

    private Fragment getProvidersFragment() {
        Fragment contentFragment;

        if(mExport) {
            contentFragment = ExportFragment.newInstance(null,
                    Utils.getExportableProviders(getSelectedProviders()),
                    Constants.LIST_VIEW);
        } else {
            contentFragment = NodesFragment.newInstance(null,
                    Utils.getProviders(getSelectedProviders()),
                    Constants.LIST_VIEW);
        }

        return contentFragment;
    }

    public void onEvent(GotContentEvent event) {
        if(!mIsWaitingForContent) {
            return;
        }

        Folder folder= event.folder;
        mIsUserAuthorized = folder.auth;
        mFolderClientCode = event.folder.client;

        if(mIsUserAuthorized) {
            showFolderContent(folder, event.backPressed);
        } else {
            showAuthFragment();
        }
    }

    private void showAuthFragment() {
        Context context = getApplicationContext();
        if(context != null) {
            Toast.makeText(this,
                    "Connecting to " + mCurrentDisplayedNode.node.displayName + " ...",
                    Toast.LENGTH_SHORT).show();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content,
                        AuthFragment.newInstance(mFolderClientCode), AUTH_FRAGMENT_TAG)
                .commit();
    }

    public void onEvent(FpFilesReceivedEvent event) {
        ArrayList<FPFile> fpFiles = event.fpFiles;

        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra(FPFILES_EXTRA, fpFiles);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public void onEvent(ApiErrorEvent event) {
        Utils.showQuickToast(this, R.string.no_internet);
        finish();
    }

    public void onEvent(SignedOutEvent event) {
        clearSession(this);
        showProvidersList(true);
    }

    public void onEvent(FileExportedEvent event) {
        String message = "File " + event.fpFile.getFilename() +
                " was exported to " + event.path.split("/")[0];

        Utils.showQuickToast(this, message);

        Intent resultIntent = new Intent();

        ArrayList<FPFile> result = new ArrayList<>();
        result.add(event.fpFile);
        resultIntent.putParcelableArrayListExtra(FPFILES_EXTRA, result);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public void proceedAfterAuth() {
       // Try getting content again
       getContent(mCurrentDisplayedNode.node, false);
    }

    private void removeAuthFragment() {
        AuthFragment authFrag =
                (AuthFragment) getSupportFragmentManager().findFragmentByTag(AUTH_FRAGMENT_TAG);

        if(authFrag != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(authFrag).commit();
        }
    }

    private void validateApiKey() {
        if(API_KEY.isEmpty()) {
            Toast.makeText(this, R.string.apikey_missing, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void pickFiles(ArrayList<Node> pickedFiles) {
        showLoading();
        ContentService.pickFiles(this, pickedFiles);
    }

    // Removes currently last node's cached file and the node itself from the list
    private void removeLastNode() {
        File file = Utils.getCacheFile(this, mCurrentDisplayedNode.node.deslashedPath());
        if(file.exists()) {
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

        getContent(displayedNode.node, isBackPressed);
    }

    private void setTitle(String title) {
        if (getActionBar() != null){
            getActionBar().setTitle(title);
        }
    }

    @Override
    public void exportFile(String filename) {
        showLoading();
        ContentService.exportFile(this, mCurrentDisplayedNode.node, mFileToExport, filename);
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
        setCameraImageUri();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        try {
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        } catch(ActivityNotFoundException e) {
            Utils.showQuickToast(this, R.string.camera_not_found);
        }
    }

    @Override
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(Constants.MIMETYPE_IMAGE).addCategory(
                Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_GET_LOCAL_FILE);
    }

    private void uploadLocalFile(Uri uri) {
        showLoading();
        ContentService.uploadFile(this, uri);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.right_slide_out_back,
                        R.anim.right_slide_in_back);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // For Camera and Gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_GETFILE:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;

            case REQUEST_CODE_EXPORT_FILE:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;

            case REQUEST_CODE_GET_LOCAL_FILE:
                if (resultCode == RESULT_OK) {
                    Utils.showQuickToast(this, R.string.uploading_image);
                    showLoading();
                    uploadLocalFile(data.getData());
                }
                break;
            case REQUEST_CODE_CAMERA:
                if (resultCode == RESULT_OK) {
                    Utils.showQuickToast(this, R.string.uploading_image);
                    showLoading();
                    uploadLocalFile(imageUri);
                }
                break;
        }
    }

    private void showLoading() {
        mIsLoading = true;
        updateLoadingView();
    }

    private void hideLoading() {
        mIsLoading = false;
        updateLoadingView();
    };

    private void updateLoadingView() {
        mProgressBar.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);
        Fragment frag = getSupportFragmentManager().findFragmentById(android.R.id.content);

        if(frag != null && frag.getView() != null) {
            frag.getView().setEnabled(!mIsLoading);
            frag.getView().setAlpha(mIsLoading ? 0.2f : 1f);
        }
    }

    private void setCameraImageUri() {
        String fileName = "" + System.currentTimeMillis() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image captured by camera");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    // Returns array of selected providers if it was provided in intent
    private String[] getSelectedProviders() {
        String[] selectedProviders = null;

        if(getIntent().hasExtra(SELECTED_PROVIDERS_EXTRA))
            selectedProviders = getIntent().getStringArrayExtra(SELECTED_PROVIDERS_EXTRA);

        return selectedProviders;
    }

    // Checks if intent has action ACTION_EXPORT_FILE and data
    private boolean isValidExportRequest() {
        Intent intent = getIntent();

        return intent.getAction() != null &&
                intent.getAction().equals(ACTION_EXPORT_FILE) &&
                intent.getData() != null;
    }

    private void showCachedNode(File nodeCachedData) {
        new AsyncTask<File, Void, Void>() {
            @Override
            protected Void doInBackground(File... params) {
                mNodeContentList = new ArrayList<Node>();
                File nodeCachedData = params[0];

                try {
                    Log.d(LOG_TAG, "Reading from file " + nodeCachedData.getAbsolutePath());
                    BufferedReader br = new BufferedReader(new FileReader(nodeCachedData));
                    String line;

                    while ((line = br.readLine()) != null) {
                        for(Node node :  new Gson().fromJson(line, Node[].class)) {
                            mNodeContentList.add(node);
                        }

                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void param) {
                refreshFragment(true);
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
    private static class CacheGotItemsTask extends AsyncTask<ArrayList<Node>, Void, Void> {

        private Context mContext;
        private Node mLastNode;

        public CacheGotItemsTask(Context context, Node lastNode) {
            this.mContext = context;
            this.mLastNode = lastNode;
        }

        @Override
        protected Void doInBackground(ArrayList<Node>... params) {
            final ArrayList<Node> nodes = params[0];
            if(nodes != null && nodes.size() > 0 && mLastNode != null) {
                Gson gson = new Gson();
                JsonElement jsonElement = new JsonParser().parse(gson.toJson(nodes));
                String result = gson.toJson(jsonElement);
                File file = Utils.getCacheFile(mContext, mLastNode.deslashedPath());
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
}
