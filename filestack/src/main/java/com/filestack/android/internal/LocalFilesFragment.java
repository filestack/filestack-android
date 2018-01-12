package com.filestack.android.internal;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.Sources;
import com.filestack.android.R;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;
import static com.filestack.android.internal.Constants.REQUEST_GALLERY;

public class LocalFilesFragment extends Fragment implements
        BackButtonListener, View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.fragment_local_files, container, false);

        baseView.findViewById(R.id.select_gallery).setOnClickListener(this);

        return baseView;
    }

    @Override
    public void onClick(View view) {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*,video/*");
        galleryIntent.setAction(Intent.ACTION_PICK);
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (hasPermissions()) {
            startActivityForResult(galleryIntent, REQUEST_GALLERY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
            ClipData clipData = data.getClipData();
            ArrayList<Uri> uris = new ArrayList<>();

            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uris.add(clipData.getItemAt(i).getUri());
                }
            } else {
                uris.add(data.getData());
            }

            for (Uri uri : uris) {
                String path = Util.getPathFromMediaUri(getContext(), uri);
                String parts[] = path.split("/");
                String name = parts[parts.length - 1];
                Util.getSelectionSaver().toggleItem(Sources.DEVICE, path, name);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private boolean hasPermissions() {
        Activity activity = getActivity();
        String[] permissions = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE };

        for (String permission : permissions) {
            int check = ContextCompat.checkSelfPermission(activity, permission);
            if (check != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, permissions, 0);
                return false;
            }
        }

        return true;
    }
}
