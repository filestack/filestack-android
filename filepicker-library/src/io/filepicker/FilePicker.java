package io.filepicker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FileExportedEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GetContentEvent;
import io.filepicker.events.SignedOutEvent;
import io.filepicker.models.FPFile;
import io.filepicker.models.Node;
import io.filepicker.models.Folder;
import io.filepicker.services.ContentService;
import io.filepicker.utils.Constants;
import io.filepicker.utils.FilesUtils;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.Utils;
import retrofit.mime.TypedFile;


public class Filepicker extends FragmentActivity
        implements AuthFragment.Contract, NodesFragment.Contract, ExportFragment.Contract {

    private static final String AUTH_FRAGMENT_TAG = "authFragment";

    public static final String NODE_EXTRA = "node";
    public static final String FPFILES_EXTRA = "fpfile";

    // We call this extra 'services' just because users are more used to this name
    public static final String SELECTED_PROVIDERS_EXTRA = "services";
    public static final String MULTIPLE_EXTRA = "multiple";
    public static final String MIMETYPE_EXTRA = "mimetype";
    public static final String LOCATION_EXTRA = "location";
    public static final String PATH_EXTRA = "path";
    public static final String CONTAINER_EXTRA = "container";
    public static final String ACCESS_EXTRA = "access";

    // This key is use when the activity shows content, not providers list
    public static final String CONTENT_EXTRA = "content";

    private static String API_KEY_STATE = "apiKeyState";
    private static String IMAGE_URI_STATE = "imageUriState";

    private static String API_KEY = "";
    private static String APP_NAME = "";

    public final static int REQUEST_CODE_GETFILE = 601;
    public final static int REQUEST_CODE_CAMERA = 602;
    public final static int REQUEST_CODE_GET_LOCAL_FILE = 603;
    public final static int REQUEST_CODE_EXPORT_FILE = 604;

    // Action used by clients to indicate they want to export file
    public final static String ACTION_EXPORT_FILE = "export_file";

    // Needed for camera request
    private Uri imageUri;
    private Node node;

    ProgressBar mProgressBar;
    boolean isAuthorized = false;
    static Uri mFileToExport;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);
        initSavedState(savedInstanceState);

        mProgressBar = (ProgressBar) findViewById(R.id.fpProgressBar);

        validateApiKey();
        initOptions();

        // Shows provider's folders and files
        if(isProvidersContentView())
            showProvidersContent();
        else
            showProvidersList();
    }

    private void initSavedState(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            setKey(savedInstanceState.getString(API_KEY_STATE));

            if(savedInstanceState.getString(IMAGE_URI_STATE) != null)
                imageUri = Uri.parse(savedInstanceState.getString(IMAGE_URI_STATE));
        }
    }

    private void initOptions() {
        Intent intent = getIntent();

        PreferencesUtils prefs = PreferencesUtils.newInstance(this);

        // Init Multiple option
        if (intent.hasExtra(MULTIPLE_EXTRA) && intent.getBooleanExtra(MULTIPLE_EXTRA, false))
            prefs.setMultiple();

        // Init choosing mimetypes
        if (intent.hasExtra(MIMETYPE_EXTRA)){
            String[] mimetypes = intent.getStringArrayExtra(MIMETYPE_EXTRA);

            if(mimetypes != null) {
                prefs.setMimetypes(mimetypes);
            }
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

        if(imageUri != null)
            outState.putString(IMAGE_URI_STATE, imageUri.toString());

        super.onSaveInstanceState(outState);
    }

    private boolean isProvidersContentView() {
        Intent intent = getIntent();
        return (intent.hasExtra(CONTENT_EXTRA) && intent.getBooleanExtra(CONTENT_EXTRA, false));
    }


    private void showProvidersList() {
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, getProvidersFragment()).commit();
        }
    }

    private Fragment getProvidersFragment() {
        Fragment contentFragment = null;

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

    private void showProvidersContent() {
        showProgressBar();
        node = getIntent().getParcelableExtra(NODE_EXTRA);

        if(getActionBar() != null)
            getActionBar().setTitle(node.getDisplayName());

        getContent();
    }

    public void getContent() {
        ContentService.getContent(this, node);
    }

    public void onEvent(GetContentEvent event) {
        hideProgressBar();

        Folder folder= event.getFolder();

        if(folder.isAuthorized()) {
            isAuthorized = true;
            displayContent(folder);
        } else {
            String client = folder.getClient();

            Toast.makeText(this,
                    "Connecting to " + node.getDisplayName() + " ...",
                    Toast.LENGTH_SHORT).show();

            addAuthFragment(client);
        }
    }

    private void displayContent(Folder folder) {
        Fragment contentFragment = getContentFragment(folder);

        if(getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, contentFragment).commit();
        }
    }


    private Fragment getContentFragment(Folder folder) {
        Fragment contentFragment;

        if(mExport) {
            contentFragment = ExportFragment.newInstance(node,
                    folder.getNodes(), folder.getViewType());
        } else {
            contentFragment = NodesFragment.newInstance(node,
                    folder.getNodes(), folder.getViewType());
        }

        return contentFragment;
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

    public void onEvent(FpFilesReceivedEvent event) {
        ArrayList<FPFile> fpFiles = event.getFpFiles();

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
        finish();
    }

    public void onEvent(FileExportedEvent event) {
        String message = "File " + event.getFpFile().getFilename() +
                " was exported to " + event.getPath().split("/")[0];

        Utils.showQuickToast(this, message);

        Intent resultIntent = new Intent();

        ArrayList<FPFile> result = new ArrayList<FPFile>();
        result.add(event.getFpFile());
        resultIntent.putParcelableArrayListExtra(FPFILES_EXTRA, result);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public void proceedAfterAuth() {
       // Check if auth fragment exists. If so, remove it
       removeAuthFragment();
       showProgressBar();

       // Try getting content again
       getContent();
    }

    private void addAuthFragment(String providerUrl) {
        if(getSupportFragmentManager().findFragmentByTag(AUTH_FRAGMENT_TAG) == null) {

            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content,
                            AuthFragment.newInstance(providerUrl), AUTH_FRAGMENT_TAG)
                    .commit();
        }

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
        ContentService.pickFiles(this, pickedFiles);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.right_slide_out_back,
                R.anim.right_slide_in_back);
    }

    @Override
    public void showContent(Node node) {
        Intent intent;
        int requestCode;

        if(mExport) {
            Uri contentUri = getIntent().getData();
            intent = new Intent(Filepicker.ACTION_EXPORT_FILE, contentUri, this, Filepicker.class);
            requestCode = Filepicker.REQUEST_CODE_EXPORT_FILE;
        } else {
            intent = new Intent(this, Filepicker.class);
            requestCode = Filepicker.REQUEST_CODE_GETFILE;
        }

        intent.putExtra(NODE_EXTRA, node);
        intent.putExtra(CONTENT_EXTRA, true);
        startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.right_slide_in,
                R.anim.right_slide_out);


    }

    @Override
    public void exportFile(String filename) {
        ContentService.exportFile(this, node, mFileToExport, filename);
    }

    @Override
    public Uri getFileToExport() {
        return mFileToExport;
    }

    @Override
    public void logoutUser(Node parentNode) {
        ContentService.logout(this, parentNode);
    }

    @Override
    public void openCamera() {
        setCameraImageUri();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    @Override
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(Constants.MIMETYPE_IMAGE).addCategory(
                Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_GET_LOCAL_FILE);
    }

    private void uploadLocalFile(Uri uri) {
        showProgressBar();
        ContentService.uploadFile(this, uri);
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
                    uploadLocalFile(data.getData());
                }
                break;
            case REQUEST_CODE_CAMERA:
                if (resultCode == RESULT_OK) {
                    Utils.showQuickToast(this, R.string.uploading_image);
                    uploadLocalFile(imageUri);
                }
                break;
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

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
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
}
