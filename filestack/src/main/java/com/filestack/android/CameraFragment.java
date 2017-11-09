package com.filestack.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.Sources;

import java.io.File;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.filestack.android.FsActivity.REQUEST_MEDIA_CAPTURE;

public class CameraFragment extends Fragment implements
        FsActivity.BackListener, View.OnClickListener {

    private static final String TYPE_PHOTO = "photo";
    private static final String TYPE_VIDEO = "video";
    private static final String PREF_PATH = "path";
    private static final String PREF_NAME= "name";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.fragment_camera, container, false);

        baseView.findViewById(R.id.take_photo).setOnClickListener(this);
        baseView.findViewById(R.id.take_video).setOnClickListener(this);

        return baseView;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        Intent cameraIntent = null;

        if (id == R.id.take_photo) {
            cameraIntent = createCameraIntent(TYPE_PHOTO);
        } else if (id == R.id.take_video) {
            cameraIntent = createCameraIntent(TYPE_VIDEO);
        }

        startActivityForResult(cameraIntent, REQUEST_MEDIA_CAPTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(getClass().getName(), MODE_PRIVATE);

        if (requestCode == REQUEST_MEDIA_CAPTURE && resultCode == RESULT_OK) {
            String path = prefs.getString(PREF_PATH, null);
            String name = prefs.getString(PREF_NAME, null);
            Util.addMediaToGallery(context, path);
            Util.getSelectionSaver().toggleItem(Sources.CAMERA, path, name);
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private Intent createCameraIntent(String source) {
        Intent intent = null;
        File file = null;

        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(getClass().getName(), MODE_PRIVATE);

        try {
            switch (source) {
                case TYPE_PHOTO:
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    file = Util.createPictureFile(context);
                    break;
                case TYPE_VIDEO:
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    file = Util.createMovieFile(context);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file != null) {
            String path = file.getAbsolutePath();
            String name = file.getName();
            prefs.edit().putString(PREF_PATH, path).putString(PREF_NAME, name).apply();
            Uri uri = Util.getUriForInternalMedia(getContext(), file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        return intent;
    }
}
