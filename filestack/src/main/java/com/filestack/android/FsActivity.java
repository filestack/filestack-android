package com.filestack.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.filestack.CloudResponse;
import com.filestack.Config;
import com.filestack.Sources;
import com.filestack.StorageOptions;
import com.filestack.android.internal.BackButtonListener;
import com.filestack.android.internal.CameraFragment;
import com.filestack.android.internal.CloudAuthFragment;
import com.filestack.android.internal.CloudListFragment;
import com.filestack.android.internal.LocalFilesFragment;
import com.filestack.android.internal.SelectionSaver;
import com.filestack.android.internal.SourceInfo;
import com.filestack.android.internal.UploadService;
import com.filestack.android.internal.Util;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/** UI to select and upload files from local and cloud sources.
 *
 * This class should be launched through the creation and sending of an {{@link Intent}}.
 * Options are set by passing values to {@link Intent#putExtra(String, String)}.
 * The keys and descriptions for these options are defined in {{@link FsConstants}}.
 *
 * There are two types of results from this activity, the files a user selects ({{@link Selection}})
 * and the metadata returned when these selections are uploaded ({{@link com.filestack.FileLink}}).
 * Automatic uploads can be disabled, in which case you will not receive any of the latter.
 *
 * User selections are returned as an {{@link ArrayList}} of {{@link Selection}} objects to
 * {{@link android.app.Activity#onActivityResult(int, int, Intent)}}. To receive upload metadata,
 * you must define and register a {{@link android.content.BroadcastReceiver}}. The corresponding
 * {{@link android.content.IntentFilter}} must be created to catch
 * {{@link FsConstants#BROADCAST_UPLOAD}}. Upload metadata is returned as
 * {{@link com.filestack.FileLink}} objects passed to
 * {{@link android.content.BroadcastReceiver#onReceive(Context, Intent)}}. The key strings needed to
 * pull results from intents are defined in {{@link FsConstants}}.
 *
 * The intent and broadcast mechanisms, and keys defined in {{@link FsConstants}}, are the contract
 * for this class. The actual code of this class should be considered internal implementation.
 */
public class FsActivity extends AppCompatActivity implements
        SingleObserver<CloudResponse>, CompletableObserver, SelectionSaver.Listener,
        NavigationView.OnNavigationItemSelectedListener {

    private static final String PREF_SESSION_TOKEN = "sessionToken";
    private static final String STATE_SELECTED_SOURCE = "selectedSource";
    private static final String STATE_SHOULD_CHECK_AUTH = "shouldCheckAuth";
    private static final String TAG = "FsActivity";

    private BackButtonListener backListener;
    private DrawerLayout drawer;
    private String selectedSource;
    private boolean shouldCheckAuth;
    private NavigationView nav;

    private boolean allowMultipleFiles;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        setContentView(R.layout.filestack__activity_filestack);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.filestack__picker_title);

        drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.filestack__nav_drawer_open, R.string.filestack__nav_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }
        nav = findViewById(R.id.nav_view);
        nav.setNavigationItemSelectedListener(this);

        List<String> sources = (List<String>) intent.getSerializableExtra(FsConstants.EXTRA_SOURCES);
        if (sources == null) {
            sources = Util.getDefaultSources();
        }

        allowMultipleFiles = intent.getBooleanExtra(FsConstants.EXTRA_ALLOW_MULTIPLE_FILES, true);

        String[] mimeTypes = intent.getStringArrayExtra(FsConstants.EXTRA_MIME_TYPES);
        if (mimeTypes != null && sources.contains(Sources.CAMERA)) {
            if (!Util.mimeAllowed(mimeTypes, "image/jpeg") && !Util.mimeAllowed(mimeTypes, "video/mp4")) {
                sources.remove(Sources.CAMERA);
                Log.w(TAG, "Hiding camera since neither image/jpeg nor video/mp4 MIME type is allowed");
            }
        }

        Menu menu = nav.getMenu();
        int index = 0;
        for (String source : sources) {
            int id = Util.getSourceIntId(source);
            SourceInfo info = Util.getSourceInfo(source);
            MenuItem item = menu.add(Menu.NONE, id, index++, info.getTextId());
            item.setIcon(info.getIconId());
            item.setCheckable(true);
        }

        if (savedInstanceState == null) {
            Config config = (Config) intent.getSerializableExtra(FsConstants.EXTRA_CONFIG);
            String sessionToken = preferences.getString(PREF_SESSION_TOKEN, null);
            Util.initializeClient(config, sessionToken);

            Util.getSelectionSaver().clear();

            selectedSource = sources.get(0);
            nav.getMenu().performIdentifierAction(Util.getSourceIntId(selectedSource), 0);
            if (drawer != null) {
                drawer.openDrawer(Gravity.START);
            }
        } else {
            selectedSource = savedInstanceState.getString(STATE_SELECTED_SOURCE);
            shouldCheckAuth = savedInstanceState.getBoolean(STATE_SHOULD_CHECK_AUTH);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Util.getSelectionSaver().setItemChangeListener(this);

        if (shouldCheckAuth) {
            checkAuth();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String sessionToken = Util.getClient().getSessionToken();
        preferences.edit().putString(PREF_SESSION_TOKEN, sessionToken).apply();
        Util.getSelectionSaver().setItemChangeListener(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_SELECTED_SOURCE, selectedSource);
        outState.putBoolean(STATE_SHOULD_CHECK_AUTH, shouldCheckAuth);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        try {
            backListener = (BackButtonListener) fragment;
        } catch (ClassCastException e) {
            backListener = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (backListener == null || !backListener.onBackPressed()){
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            SourceInfo info = Util.getSourceInfo(selectedSource);
            Util.getClient()
                    .logoutCloudAsync(info.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this);
            return true;
        } else if (id == R.id.action_upload) {
            uploadSelections(Util.getSelectionSaver().getItems());
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filestack__menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_upload).setVisible(!Util.getSelectionSaver().isEmpty());
        return true;
    }

    @Override
    public void onComplete() {
        checkAuth();
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        // TODO Error handling
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String source = Util.getSourceStringId(id);
        Fragment fragment = null;

        if (!source.equals(selectedSource)) {
            Util.getSelectionSaver().clear();
        }

        SourceInfo sourceInfo = Util.getSourceInfo(source);
        getSupportActionBar().setSubtitle(sourceInfo.getTextId());

        selectedSource = source;
        nav.setCheckedItem(id);

        switch (source) {
            case Sources.CAMERA:
                fragment = new CameraFragment();
                // Needed to prevent UI bug when selecting local source after cloud source
                shouldCheckAuth = false;
                break;
            case Sources.DEVICE:
                fragment = LocalFilesFragment.newInstance(allowMultipleFiles);
                // Needed to prevent UI bug when selecting local source after cloud source
                shouldCheckAuth = false;
                break;
            default:
                checkAuth();
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
        }

        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    @Override
    public void onEmptyChanged(boolean isEmpty) {
        invalidateOptionsMenu();
    }

    @Override
    public void onSubscribe(Disposable d) { }

    @Override
    public void onSuccess(CloudResponse contents) {
        String authUrl = contents.getAuthUrl();

        // TODO Switching source views shouldn't depend on a network request

        if (authUrl != null) {
            shouldCheckAuth = true;
            CloudAuthFragment cloudAuthFragment = CloudAuthFragment.create(selectedSource, authUrl);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudAuthFragment);
            transaction.commit();
        } else {
            shouldCheckAuth = false;
            CloudListFragment cloudListFragment = CloudListFragment.create(selectedSource, allowMultipleFiles);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudListFragment);
            transaction.commit();
        }
    }

    private void checkAuth() {
        SourceInfo info = Util.getSourceInfo(selectedSource);
        Util.getClient()
                .getCloudItemsAsync(info.getId(), "/")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    private void uploadSelections(ArrayList<Selection> selections) {
        Intent activityIntent = getIntent();
        boolean autoUpload = activityIntent.getBooleanExtra(FsConstants.EXTRA_AUTO_UPLOAD, true);

        if (autoUpload) {
            StorageOptions storeOpts = (StorageOptions) activityIntent
                    .getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);
            Intent uploadIntent = new Intent(this, UploadService.class);
            uploadIntent.putExtra(FsConstants.EXTRA_STORE_OPTS, storeOpts);
            uploadIntent.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);
            startService(uploadIntent);
        }

        Intent data = new Intent();
        data.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);
        setResult(RESULT_OK, data);
        finish();
    }
}
