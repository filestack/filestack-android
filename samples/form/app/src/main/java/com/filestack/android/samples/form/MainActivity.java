package com.filestack.android.samples.form;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.filestack.FileLink;
import com.filestack.android.FsConstants;

public class MainActivity extends AppCompatActivity {

    private UploadReceiver uploadReceiver;
    private LoadingFragment loadingFragment;
    private FormFragment formFragment;
    private FileLink fileLink;




    // Receives upload broadcasts from SDK service
    public class UploadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);
            setLoading(false);
        }
    }




    // Swap between showing the form and loading fragments
    public void setLoading(boolean isLoading) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.root, isLoading ? loadingFragment : formFragment)
                .commit();
    }

    public FileLink getFileLink() {
        return fileLink;
    }

    // Show the complete fragment when an account has been created
    public void setComplete(String name) {
        CompleteFragment fragment = CompleteFragment.create(name);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.root, fragment)
                .commit();
    }

    // Remove the complete fragment and show an empty form fragment
    public void reset() {
        fileLink = null;
        formFragment = new FormFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.root, formFragment)
                .commit();
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create fragments
        loadingFragment = new LoadingFragment();
        formFragment = new FormFragment();

        // Register the receiver for upload broadcasts
        IntentFilter filter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
        uploadReceiver = new UploadReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver, filter);

        if (savedInstanceState == null) {
            // Initial attachment of the form fragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.root, formFragment)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver to avoid leaking it outside tne activity context
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
    }

}
