package com.filestack.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.view.View;

import com.filestack.Client;
import com.filestack.CloudResponse;
import com.filestack.Config;

import java.util.ArrayList;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FilestackActivity extends AppCompatActivity implements
        SingleObserver<CloudResponse>, CompletableObserver, SelectedItem.Saver.ItemChangeListener,
        NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_CONFIG = "config";

    interface BackListener {
        boolean onBackPressed();
    }

    private static final int REQUEST_CAMERA = RESULT_FIRST_USER;
    private static final int REQUEST_FILE_BROWSER = RESULT_FIRST_USER + 1;
    private static final String PREF_SELECTED_SOURCE_ID = "selectedSourceId";
    private static final String PREF_SESSION_TOKEN = "sessionToken";

    private BackListener backListener;
    private DrawerLayout drawer;
    private int selectedSourceId;
    private NavigationView nav;
    private Toolbar toolbar;

    // Activity lifecycle overrides (in sequential order)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Config config = (Config) intent.getSerializableExtra(EXTRA_CONFIG);
        Util.setClient(new Client(config));

        // If we're starting fresh, clear selected items
        if (savedInstanceState == null) {
            Util.getItemSaver().clear();
        }

        Util.getItemSaver().setItemChangeListener(this);

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
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String sessionToken = preferences.getString(PREF_SESSION_TOKEN, null);
        Log.d("sessionToken", "Retrieving: " + sessionToken);
        Util.getClient().setSessionToken(sessionToken);
        selectedSourceId = preferences.getInt(PREF_SELECTED_SOURCE_ID, 0);
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
        Util.getItemSaver().setItemChangeListener(null);
    }

    // Other Activity overrides (alphabetical order)

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        try {
            backListener = (BackListener) fragment;
        } catch (ClassCastException e) { }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.filestack, menu);
        return true;
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
            Intent uploadIntent = new Intent(this, UploadService.class);
            ArrayList<SelectedItem> items = Util.getItemSaver().getItems();
            uploadIntent.putParcelableArrayListExtra(UploadService.EXTRA_SELECTED_ITEMS, items);
            startService(uploadIntent);
            Util.getItemSaver().clear();
            finish();
        }

        return super.onOptionsItemSelected(item);
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

        if (id == R.id.nav_camera) {
            Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        } else if (id == R.id.nav_device) {
            Intent fileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileBrowserIntent.setType("*/*");
            if (fileBrowserIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(fileBrowserIntent, REQUEST_FILE_BROWSER);
            }
        } else {
            if (id != selectedSourceId) {
                Util.getItemSaver().clear();
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
    public void onCountChanged(int newSize) {
        toolbar.getMenu().findItem(R.id.action_upload).setVisible(newSize > 0);
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
}
