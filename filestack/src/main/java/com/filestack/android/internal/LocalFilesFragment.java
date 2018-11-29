package com.filestack.android.internal;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ImageViewCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.filestack.android.FsConstants;
import com.filestack.android.R;
import com.filestack.android.Selection;
import com.filestack.android.Theme;

import java.util.ArrayList;

import static android.app.Activity.RESULT_FIRST_USER;

/**
 * Handles opening system file browser and processing results for local file selection.
 *
 * @see <a href="https://developer.android.com/guide/topics/providers/document-provider">
 *     https://developer.android.com/guide/topics/providers/document-provider</a>
 */
public class LocalFilesFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_ALLOW_MULTIPLE_FILES = "multipleFiles";
    private static final int READ_REQUEST_CODE = RESULT_FIRST_USER;
    private static final String ARG_THEME = "theme";

    public static Fragment newInstance(boolean allowMultipleFiles, Theme theme) {
        Fragment fragment = new LocalFilesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_MULTIPLE_FILES, allowMultipleFiles);
        args.putParcelable(ARG_THEME, theme);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.filestack__fragment_local_files, container, false);
        Button openGalleryButton = view.findViewById(R.id.select_gallery);
        openGalleryButton.setOnClickListener(this);
        Theme theme = getArguments().getParcelable(ARG_THEME);
        ViewCompat.setBackgroundTintList(openGalleryButton, ColorStateList.valueOf(theme.getAccentColor()));
        openGalleryButton.setTextColor(theme.getBackgroundColor());
        ImageViewCompat.setImageTintList((ImageView) view.findViewById(R.id.icon), ColorStateList.valueOf(theme.getTextColor()));
        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_toggle_list_grid).setVisible(false);
    }

    @Override
    public void onClick(View view) {
        startFilePicker();
    }

    private void startFilePicker() {
        final Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            boolean allowMultipleFiles = getArguments().getBoolean(ARG_ALLOW_MULTIPLE_FILES, true);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleFiles);
            intent.setType("*/*");

            Intent launchIntent = getActivity().getIntent();
            String[] mimeTypes = launchIntent.getStringArrayExtra(FsConstants.EXTRA_MIME_TYPES);
            if (mimeTypes != null) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
            startActivityForResult(intent, READ_REQUEST_CODE);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ClipData clipData = resultData.getClipData();
            ArrayList<Uri> uris = new ArrayList<>();

            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uris.add(clipData.getItemAt(i).getUri());
                }
            } else {
                uris.add(resultData.getData());
            }

            for (Uri uri : uris) {
                Selection selection = processUri(uri);
                Util.getSelectionSaver().toggleItem(selection);
            }
        }
    }

    private Selection processUri(Uri uri) {
        ContentResolver resolver = getActivity().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, null, null, null, null, null);
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
                return SelectionFactory.from(uri, size, mimeType, name);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
