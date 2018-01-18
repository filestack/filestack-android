package com.filestack.android.internal;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import com.filestack.Sources;
import com.filestack.android.R;
import com.filestack.android.Selection;

import java.util.ArrayList;

import static android.app.Activity.RESULT_FIRST_USER;

public class LocalFilesFragment extends Fragment implements View.OnClickListener {
    private static final int READ_REQUEST_CODE = RESULT_FIRST_USER;
    private static final String TAG = "LocalFilesFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_local_files, container, false);
        view.findViewById(R.id.select_gallery).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    // Receive selected documents and process them
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ClipData clipData = resultData.getClipData();
            ArrayList<Uri> uris = new ArrayList<>();

            // Multiple documents were selected
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uris.add(clipData.getItemAt(i).getUri());
                }
            } else { // Single document was selected
                uris.add(resultData.getData());
            }

            // Process documents
            for (Uri uri : uris) {
                Selection selection = processUri(uri);
                Util.getSelectionSaver().toggleItem(selection);
            }
        }
    }

    // Get metadata for specified URI and return it loaded into Selection instance
    public Selection processUri(Uri uri) {
        ContentResolver resolver = getActivity().getContentResolver();

        try (Cursor cursor = resolver.query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                // We can't upload files without knowing the size
                if (cursor.isNull(sizeIndex)) {
                    return null;
                }

                String name = cursor.getString(nameIndex);
                int size = cursor.getInt(sizeIndex);
                String mimeType = resolver.getType(uri);

                return new Selection(Sources.DEVICE, uri, size, mimeType, name);
            } else {
                return null;
            }
        }
    }
}
