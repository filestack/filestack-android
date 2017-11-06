package com.filestack.android.demo;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.filestack.Config;
import com.filestack.StorageOptions;
import com.filestack.android.FsConstants;
import com.filestack.android.FsActivity;
import com.filestack.android.Selection;

import java.io.Serializable;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FILESTACK = RESULT_FIRST_USER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intentFilter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
        UploadStatusReceiver receiver = new UploadStatusReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILESTACK && resultCode == RESULT_OK) {
            Log.d("result", "returned");
            Serializable extra = data.getSerializableExtra(FsConstants.EXTRA_SELECTION_LIST);
            ArrayList<Selection> selections = (ArrayList<Selection>) extra;
            for (Selection selection : selections) {
                Log.i("filestackSelection", selection.getProvider() + " " + selection.getName());
            }
        }
    }

    public void openFilestack(View view) {
        Intent intent = new Intent(this, FsActivity.class);
        Config config = new Config(
                getString(R.string.api_key),
                getString(R.string.return_url),
                getString(R.string.policy),
                getString(R.string.signature));
        StorageOptions storeOpts = new StorageOptions();
        intent.putExtra(FsConstants.EXTRA_CONFIG, config);
        intent.putExtra(FsConstants.EXTRA_STORE_OPTS, storeOpts);
        intent.putExtra(FsConstants.EXTRA_AUTO_UPLOAD, true);
        startActivityForResult(intent, REQUEST_FILESTACK);
    }

    public void openFileBrowser(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 0);
    }
}
