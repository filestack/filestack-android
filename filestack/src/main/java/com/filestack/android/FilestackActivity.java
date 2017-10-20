package com.filestack.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
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

import com.filestack.CloudContents;
import com.filestack.Security;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class FilestackActivity extends AppCompatActivity implements
        SingleObserver<CloudContents>, CompletableObserver, ClientProvider,
        NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_API_KEY = "apiKey";
    public static final String EXTRA_POLICY = "policy";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_APP_URL = "appUrl";

    private static final int REQUEST_CAMERA = RESULT_FIRST_USER;
    private static final int REQUEST_FILE_BROWSER = RESULT_FIRST_USER + 1;

    private static final String PREF_SESSION_TOKEN = "sessionToken";
    private static final String PREF_SELECTED_SOURCE_ID = "selectedSourceId";

    private DrawerLayout drawer;
    private NavigationView nav;
    private Toolbar toolbar;
    private FilestackAndroidClient client;

    private int selectedSourceId; // TODO maybe don't do this
    private boolean checkAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        String apiKey = intent.getStringExtra(EXTRA_API_KEY);
        String policy = intent.getStringExtra(EXTRA_POLICY);
        String signature = intent.getStringExtra(EXTRA_SIGNATURE);
        String appUrl = intent.getStringExtra(EXTRA_APP_URL);

        Security security = null;

        if (policy != null && signature != null) {
            security = Security.fromExisting(policy, signature);
        }

        client = new FilestackAndroidClient(apiKey, security);
        client.setReturnUrl(appUrl);

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
        client.setSessionToken(sessionToken);
        selectedSourceId = preferences.getInt(PREF_SELECTED_SOURCE_ID, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String sessionToken = client.getSessionToken();
        Log.d("sessionToken", "Saving: " + sessionToken);
        preferences
                .edit()
                .putString(PREF_SESSION_TOKEN, sessionToken)
                .putInt(PREF_SELECTED_SOURCE_ID, selectedSourceId)
                .apply();
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
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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

        Log.d("menu item click", "activity");

        if (id == R.id.action_logout) {
            SourceInfo info = Util.getSourceInfo(selectedSourceId);
            client
                    .logoutCloudAsync(info.getId())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
    public void onSubscribe(Disposable d) { }

    @Override
    public void onSuccess(CloudContents contents) {
        String authUrl = contents.getAuthUrl();

        if (authUrl != null) {
            AuthFragment authFragment = AuthFragment.create(selectedSourceId, authUrl);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, authFragment);
            transaction.commit();
        } else {
            checkAuth = false;
            CloudListFragment cloudListFragment = CloudListFragment.create(selectedSourceId);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudListFragment);
            transaction.commit();
        }
    }

    @Override
    public void onComplete() {
        checkAuth();
    }

    @Override
    public void onError(Throwable e) { }

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

    private void checkAuth() {
        checkAuth = true;
        SourceInfo info = Util.getSourceInfo(selectedSourceId);
        client
                .getCloudContentsAsync(info.getId(), "/")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
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

    @Override
    public FilestackAndroidClient getClient() {
        return client;
    }
}
