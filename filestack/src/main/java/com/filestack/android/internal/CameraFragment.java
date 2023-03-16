package com.filestack.android.internal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.filestack.Sources;
import com.filestack.android.FsConstants;
import com.filestack.android.R;
import com.filestack.android.Selection;

import java.io.File;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.filestack.android.internal.Constants.REQUEST_MEDIA_CAPTURE;

/**
 * Handles launching intents to capture photos and videos using the device's default camera app.
 */
public class CameraFragment extends Fragment implements BackButtonListener, View.OnClickListener {

    private static final String TYPE_PHOTO = "photo";
    private static final String TYPE_VIDEO = "video";
    private static final String PREF_PATH = "path";
    private static final String PREF_NAME= "name";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.filestack__fragment_camera, container, false);
        Button photoButton = root.findViewById(R.id.take_photo);
        Button videoButton = root.findViewById(R.id.take_video);

        Intent intent = requireActivity().getIntent();
        String[] mimeTypes = intent.getStringArrayExtra(FsConstants.EXTRA_MIME_TYPES);

        // Disable buttons if associated MIME type isn't allowed
        if (mimeTypes != null) {
            if (!Util.mimeAllowed(mimeTypes, "image/jpeg")) {
                photoButton.setVisibility(View.GONE);
            }
            if (!Util.mimeAllowed(mimeTypes, "video/mp4")) {
                videoButton.setVisibility(View.GONE);
            }
        }

        photoButton.setOnClickListener(this);
        videoButton.setOnClickListener(this);

        return root;
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
            String mimeType = name.contains("jpg") ? "image/jpeg" : "video/mp4";
            Util.addMediaToGallery(context, path);
            Selection selection = new Selection(Sources.CAMERA, path, mimeType, name);
            Util.getSelectionSaver().toggleItem(selection);
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_toggle_list_grid).setVisible(false);
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
