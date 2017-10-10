package com.filestack.android;

import android.content.Intent;
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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.filestack.CloudContents;
import com.filestack.Security;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class FilestackActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SingleObserver<CloudContents> {

    private static final int REQUEST_CAMERA = RESULT_FIRST_USER;
    private static final int REQUEST_FILE_BROWSER = RESULT_FIRST_USER + 1;

    private DrawerLayout drawer;
    private NavigationView nav;
    private FilestackAndroidClient client;

    // TODO maybe don't do this
    private int selectedCloud;
    private boolean checkAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup client here;
        FilestackAndroidClient client;

        setContentView(R.layout.activity_filestack);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        nav = (NavigationView) findViewById(R.id.nav_view);
        nav.setNavigationItemSelectedListener(this);
        nav.setItemIconTintList(null);
        setNavIconColors();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (selectedCloud == 0) {
            nav.getMenu().performIdentifierAction(R.id.nav_facebook, 0);
            drawer.openDrawer(Gravity.START);
        } else if (checkAuth) {
            checkAuth(selectedCloud);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.filestack, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        // if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        nav.setCheckedItem(id);

        if (id == R.id.nav_camera) {
            checkAuth = false;
            Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        } else if (id == R.id.nav_file_browser) {
            checkAuth = false;
            Intent fileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileBrowserIntent.setType("*/*");
            if (fileBrowserIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(fileBrowserIntent, REQUEST_FILE_BROWSER);
            }
        } else if (id != selectedCloud){
            checkAuth = true;
            selectedCloud = id;
            checkAuth(selectedCloud);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setNavIconColors() {
        Menu menu = nav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            Menu subMenu = menu.getItem(i).getSubMenu();
            for (int j = 0; j < subMenu.size(); j++) {
                MenuItem item = subMenu.getItem(j);
                Drawable icon = item.getIcon().mutate();
                CloudInfo res = Util.getCloudInfo(item.getItemId());
                icon.setColorFilter(res.getIconId(), PorterDuff.Mode.MULTIPLY);
                subMenu.getItem(j).setIcon(icon);
            }
        }
    }

    private void checkAuth(int id) {
        CloudInfo info = Util.getCloudInfo(id);
        client.getCloudContentsAsync(info.getProvider(), "/").subscribe(this);
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onSuccess(CloudContents contents) {
        String authUrl = contents.getAuthUrl();

        if (authUrl != null) {
            AuthFragment authFragment = AuthFragment.create(selectedCloud, authUrl);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, authFragment);
            transaction.commit();
        } else {
            checkAuth = false;
            CloudListFragment cloudListFragment = CloudListFragment.create(selectedCloud);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudListFragment);
            transaction.commit();
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }
}
