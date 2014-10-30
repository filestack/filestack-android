package io.filepicker;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.filepicker.events.ApiErrorEvent;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.events.GetContentEvent;
import io.filepicker.events.SignedOutEvent;
import io.filepicker.models.FPFile;
import io.filepicker.models.Node;
import io.filepicker.models.Folder;
import io.filepicker.services.ContentService;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.Utils;


public class Filepicker extends FragmentActivity
        implements AuthFragment.Contract, NodesFragment.Contract {

    private static final String AUTH_FRAGMENT_TAG = "authFragment";

    public static final String NODE_EXTRA = "node";
    public static final String FPFILES_EXTRA = "fpfile";

    // We call this extra 'services' just because users are more used to this name
    public static final String SELECTED_PROVIDERS_EXTRA = "services";
    public static final String MULTIPLE_EXTRA = "multiple";

    // This key is use when the activity shows content, not providers list
    public static final String CONTENT_EXTRA = "content";

    private static String API_KEY_STATE = "apiKeyState";

    private static String API_KEY = "";
    private static String APP_NAME = "";

    public final static int REQUEST_CODE_GETFILE = 601;
    public final static int REQUEST_CODE_CAMERA = 602;
    public final static int REQUEST_CODE_GET_LOCAL_FILE = 603;

    // Needed for camera request
    private Uri imageUri;
    private Node node;

    ProgressBar mProgressBar;
    boolean isAuthorized = false;

    public static void setKey(String apiKey) {
        if(API_KEY.isEmpty()) {
            API_KEY = apiKey;
        }
    }

    public static String getApiKey() {
        return API_KEY;
    }

    public static void setAppName(String appName) {
        APP_NAME = appName;
    }

    public static String getAppName() {
        if(!APP_NAME.isEmpty()) {
            return APP_NAME;
        }
        return Resources.getSystem().getString(R.string.app_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);

        // Restore api key if it was stored
        if(savedInstanceState != null) {
            setKey(savedInstanceState.getString(API_KEY_STATE));
        }

        validateApiKey();
        initOptions();


        mProgressBar = (ProgressBar) findViewById(R.id.fpProgressBar);

        // It means user sees the list of providers, like Facebook, Gallery etc.
        if(isProvidersView()) {
            showProvidersList();
        } else {
            showProgressBar();
            node = getIntent().getParcelableExtra(NODE_EXTRA);
            getActionBar().setTitle(node.getDisplayName());

            getContent();
        }
    }

    private void initOptions() {
        Intent intent = getIntent();

        if (intent.hasExtra(MULTIPLE_EXTRA) && intent.getBooleanExtra(MULTIPLE_EXTRA, false)) {
            PreferencesUtils.newInstance(this).setMultiple();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save api key in case the activity was destroyed
        outState.putString(API_KEY_STATE, getApiKey());
        super.onSaveInstanceState(outState);
    }

    // Provider view is the first view when user launches Filepicker library.
    // It shows a list of providers
    private boolean isProvidersView() {
        Intent intent = getIntent();

        if(intent.hasExtra(CONTENT_EXTRA) && intent.getBooleanExtra(CONTENT_EXTRA, false))
            return false;

        return true;
    }

    private void showProvidersList() {
        String[] selectedProviders = null;

        if(getIntent().hasExtra(SELECTED_PROVIDERS_EXTRA))
            selectedProviders = getIntent().getStringArrayExtra(SELECTED_PROVIDERS_EXTRA);

        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction().add(android.R.id.content,
                    NodesFragment.newInstance(null, Utils.getProviders(selectedProviders), NodesFragment.LIST_VIEW))
                    .commit();
        }
    }


    public void getContent() {
        ContentService.getContent(this, node);
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

    public void onEvent(GetContentEvent event) {
        hideProgressBar();

        Folder folder= event.getFolder();

        if(folder.isAuthorized()) {
            isAuthorized = true;
            displayContent(folder);
        } else {
            String client = folder.getClient();

            Toast.makeText(this,
                    getAppName() + " is connecting to " + node.getDisplayName() + " ...",
                    Toast.LENGTH_SHORT).show();

            addAuthFragment(client);
        }
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


    private void displayContent(Folder folder) {
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction().add(android.R.id.content,
                    NodesFragment.newInstance(node, folder.getNodes(), folder.getViewType()))
                    .commit();
        }
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
        Intent contentIntent = new Intent(this, Filepicker.class);
        contentIntent.putExtra(Filepicker.CONTENT_EXTRA, true);
        contentIntent.putExtra(Filepicker.NODE_EXTRA, node);
        startActivityForResult(contentIntent, Filepicker.REQUEST_CODE_GETFILE);
        overridePendingTransition(R.anim.right_slide_in,
                R.anim.right_slide_out);

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
        intent.setType(Utils.MIMETYPE_IMAGE).addCategory(
                Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_GET_LOCAL_FILE);
    }

    private void uploadLocalFile(Uri uri) {
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
}
