package com.filestack.android.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        int id = com.filestack.android.R.string.filestack__auth_action;
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment, new SettingsFragment())
                .commit();
    }
}
