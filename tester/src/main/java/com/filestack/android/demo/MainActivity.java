package com.filestack.android.demo;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.filestack.Config;
import com.filestack.android.FilestackPicker;
import com.filestack.android.FsConstants;
import com.filestack.android.Selection;
import com.filestack.android.Theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FILESTACK = RESULT_FIRST_USER;
    private static final int REQUEST_SETTINGS = REQUEST_FILESTACK + 1;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            IntentFilter intentFilter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
            ScrollView scrollView = findViewById(R.id.scrollView);
            TextView logView = findViewById(R.id.log);
            UploadStatusReceiver receiver = new UploadStatusReceiver(logView, scrollView);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FilestackPicker.canReadResult(requestCode, resultCode)) {
            Log.i(TAG, "received filestack selections");
            List<Selection> selections = FilestackPicker.getSelectedFiles(data);
            for (int i = 0; i < selections.size(); i++) {
                Selection selection = selections.get(i);
                String msg = String.format(Locale.ROOT, "selection %d: %s", i, selection.getName());
                Log.i(TAG, msg);
            }
        }
    }

    public void settings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    public void launch(View view) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String mimeFilter = sharedPref.getString("mime_filter", null);
        List<String> mimeTypes = Arrays.asList(mimeFilter.split(","));

        String apiKey = sharedPref.getString("api_key", null);
        String policy = sharedPref.getString("policy", null);
        String signature = sharedPref.getString("signature", null);

        if (apiKey == null) {
            Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
            return;
        }
        Config config = new Config(apiKey, getString(R.string.return_url), policy, signature);

        Theme theme = new Theme.Builder()
                .title(sharedPref.getString("theme_title", null))
                .accentColor(sharedPref.getInt("theme_accent", ContextCompat.getColor(this, R.color.colorAccent)))
                .backgroundColor(sharedPref.getInt("theme_background", ContextCompat.getColor(this, R.color.colorPrimary)))
                .textColor(sharedPref.getInt("theme_text", ContextCompat.getColor(this, R.color.text)))
                .build();

        FilestackPicker picker = new FilestackPicker.Builder()
                .config(config)
                .sources(new ArrayList<>(sharedPref.getStringSet("upload_sources", Collections.<String>emptySet())))
                .autoUploadEnabled(sharedPref.getBoolean("auto_upload", false))
                .mimeTypes(mimeTypes)
                .multipleFilesSelectionEnabled(sharedPref.getBoolean("allow_multiple_files", true))
                .theme(theme)
                .build();
        picker.launch(this);
    }
}
