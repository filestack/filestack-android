package com.filestack.android;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.filestack.CloudResponse;
import com.filestack.Config;
import com.filestack.Sources;
import com.filestack.StorageOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FsActivity extends AppCompatActivity implements
        SingleObserver<CloudResponse>, CompletableObserver, Selection.Saver.Listener,
        NavigationView.OnNavigationItemSelectedListener {

    interface BackListener {
        boolean onBackPressed();
    }

    private static final int REQUEST_MEDIA_CAPTURE = RESULT_FIRST_USER;
    private static final int REQUEST_FILE_BROWSER = RESULT_FIRST_USER + 1;
    private static final int REQUEST_GALLERY = RESULT_FIRST_USER + 2;
    private static final String PREF_SELECTED_SOURCE_ID = "selectedSourceId";
    private static final String PREF_SESSION_TOKEN = "sessionToken";

    private BackListener backListener;
    private DrawerLayout drawer;
    private int selectedSourceId;
    private NavigationView nav;
    private Selection mediaSelection;
    private Toolbar toolbar;

    // Activity lifecycle overrides (in sequential order)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        // If we're starting fresh
        if (savedInstanceState == null) {
            // Initialize static client
            Config config = (Config) intent.getSerializableExtra(FsConstants.EXTRA_CONFIG);
            String sessionToken = preferences.getString(PREF_SESSION_TOKEN, null);
            Log.d("sessionToken", "Retrieving: " + sessionToken);
            Util.initializeClient(config, sessionToken);

            // Clear selected item list
            Util.getSelectionSaver().clear();
        }

        selectedSourceId = preferences.getInt(PREF_SELECTED_SOURCE_ID, 0);
        Util.getSelectionSaver().setItemChangeListener(this);

        // Setup view
        setContentView(R.layout.activity_filestack);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        nav = (NavigationView) findViewById(R.id.nav_view);
        nav.setNavigationItemSelectedListener(this);
        // nav.setItemIconTintList(null); // To enable color icons
        // setNavIconColors();

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (selectedSourceId == 0) {
            nav.getMenu().performIdentifierAction(R.id.google_drive, 0);
            if (drawer != null) {
                drawer.openDrawer(Gravity.START);
            }
        } else {
            nav.getMenu().performIdentifierAction(selectedSourceId, 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String sessionToken = Util.getClient().getSessionToken();
        Log.d("sessionToken", "Saving: " + sessionToken);
        preferences
                .edit()
                .putString(PREF_SESSION_TOKEN, sessionToken)
                .putInt(PREF_SELECTED_SOURCE_ID, selectedSourceId)
                .apply();
        Util.getSelectionSaver().setItemChangeListener(null);
    }

    // Other Activity overrides (alphabetical order)


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ArrayList<Selection> selections = new ArrayList<>();

        switch (requestCode) {
            case REQUEST_MEDIA_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Util.addMediaToGallery(this, mediaSelection.getPath());
                    selections.add(mediaSelection);
                    uploadSelections(selections);
                }
                break;
//            case REQUEST_FILE_BROWSER:
//                if (resultCode == RESULT_OK) {
//                    ArrayList<Uri> uris = new ArrayList<>();
//                    ClipData clipData = data.getClipData();
//
//                    if (clipData != null) {
//                        for (int i = 0; i < clipData.getItemCount(); i++) {
//                            uris.add(clipData.getItemAt(i).getUri());
//                        }
//                    } else {
//                        uris.add(data.getData());
//                    }
//
//                    for (Uri uri : uris) {
//                        Log.d("localFile", uri.toString());
//                        String path = Util.getPathFromMediaUri(this, uri);
//                        String[] parts = path.split("/");
//                        String name = parts[parts.length-1];
//                        Selection selection = new Selection(Sources.DEVICE, path, name);
//                        Log.d("localFile", path + " " + name);
//                        selections.add(selection);
//                    }
//                }
//                uploadSelections(selections);
//                break;
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String path = Util.getPathFromMediaUri(this, uri);
                    String parts[] = path.split("/");
                    String name = parts[parts.length - 1];
                    selections.add(new Selection(Sources.DEVICE, path, name));
                    uploadSelections(selections);
                }
                break;
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        try {
            backListener = (BackListener) fragment;
        } catch (ClassCastException e) {
            String name = fragment.getClass().getName();
            throw new RuntimeException(name + " must implement BackListener!");
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!backListener.onBackPressed()){
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            SourceInfo info = Util.getSourceInfo(selectedSourceId);
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
        getMenuInflater().inflate(R.menu.filestack, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_upload).setVisible(!Util.getSelectionSaver().isEmpty());
        return true;
    }

    // Interface overrides (alphabetical order)

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

        if (id == R.id.nav_camera_picture || id == R.id.nav_camera_movie) {
            Intent intent = null;
            File file = null;

            try {
                if (id == R.id.nav_camera_picture) {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    file = Util.createPictureFile(this);
                } else {
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    file = Util.createMovieFile(this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (file != null) {
                mediaSelection = new Selection(
                        Sources.CAMERA, file.getAbsolutePath(), file.getName());
                Uri imageUri = FileProvider.getUriForFile(
                        this, "com.filestack.android.fileprovider", file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_MEDIA_CAPTURE);
            }
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent();
            intent.setType("image/*,video/*");
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(intent, REQUEST_GALLERY);
//        } else if (id == R.id.nav_device) {
//            Intent fileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT);
//            fileBrowserIntent.setType("*/*");
//            fileBrowserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//            if (fileBrowserIntent.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(fileBrowserIntent, REQUEST_FILE_BROWSER);
//            }
        } else {
            if (id != selectedSourceId) {
                Util.getSelectionSaver().clear();
            }

            nav.setCheckedItem(id);
            selectedSourceId = id;
            // setThemeColor();

            checkAuth();
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

        if (authUrl != null) {
            CloudAuthFragment cloudAuthFragment = CloudAuthFragment.create(selectedSourceId, authUrl);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudAuthFragment);
            transaction.commit();
        } else {
            CloudListFragment cloudListFragment = CloudListFragment.create(selectedSourceId);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudListFragment);
            transaction.commit();
        }
    }

    // Private helper methods (alphabetical order)

    private void checkAuth() {
        SourceInfo info = Util.getSourceInfo(selectedSourceId);
        Util.getClient()
                .getCloudItemsAsync(info.getId(), "/")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    private void setNavIconColors() {
        Menu menu = nav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            Menu subMenu = menu.getItem(i).getSubMenu();
            for (int j = 0; j < subMenu.size(); j++) {
                MenuItem item = subMenu.getItem(j);
                Drawable icon = item.getIcon().mutate();
                SourceInfo res = Util.getSourceInfo(item.getItemId());
                icon.setColorFilter(res.getIconId(), PorterDuff.Mode.MULTIPLY);
                subMenu.getItem(j).setIcon(icon);
            }
        }
    }

    private void setThemeColor() {
        SourceInfo info = Util.getSourceInfo(selectedSourceId);
        View header = nav.getHeaderView(0);
        if (header != null) {
            header.setBackgroundResource(info.getColorId());
        }
        toolbar.setBackgroundResource(info.getColorId());
        if (drawer != null) {
            toolbar.setSubtitle(info.getTextId());
        }
    }

    private void uploadSelections(ArrayList<Selection> selections) {
        Intent uploadIntent = new Intent(this, UploadService.class);

        StorageOptions storeOpts =
                (StorageOptions) getIntent().getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);

        uploadIntent.putExtra(FsConstants.EXTRA_STORE_OPTS, storeOpts);
        uploadIntent.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);

        startService(uploadIntent);

        Intent data = new Intent();
        data.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);
        setResult(RESULT_OK, data);
        finish();
    }

    private void checkPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }
}
