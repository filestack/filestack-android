package com.filestack.android;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.filestack.Config;
import com.filestack.StorageOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilestackPicker {

    /**
     * Checks whether result acquired in {@link Activity#onActivityResult(int, int, Intent)}
     *     can be read.
     * @param requestCode requestCode from {@link Activity#onActivityResult(int, int, Intent)}
     * @param resultCode resultCode from {@link Activity#onActivityResult(int, int, Intent)}
     */
    public static boolean canReadResult(int requestCode, int resultCode) {
        return requestCode == FILESTACK_REQUEST_CODE && resultCode == Activity.RESULT_OK;
    }

    /**
     * Extracts list of files from result in {@link Activity#onActivityResult(int, int, Intent)}.
     * Check with {@link #canReadResult(int, int)} should be made before using this method.
     * @param data - intent received in {@link Activity#onActivityResult(int, int, Intent)}
     * @return list of selected files ({@link Selection})
     */
    public static List<Selection> getSelectedFiles(Intent data) {
        if (!data.hasExtra(FsConstants.EXTRA_SELECTION_LIST)) {
            return Collections.emptyList();
        }
        return data.getParcelableArrayListExtra(FsConstants.EXTRA_SELECTION_LIST);
    }

    private static final int FILESTACK_REQUEST_CODE = 20181130;

    private final Config config;
    private final StorageOptions storageOptions;
    private final boolean autoUpload;
    private final List<String> sources;
    private final List<String> mimeTypes;
    private final boolean allowMultipleFiles;

    private FilestackPicker(Config config, StorageOptions storageOptions, boolean autoUpload, List<String> sources, List<String> mimeTypes, boolean allowMultipleFiles) {
        this.config = config;
        this.storageOptions = storageOptions;
        this.autoUpload = autoUpload;
        this.sources = sources;
        this.mimeTypes = mimeTypes;
        this.allowMultipleFiles = allowMultipleFiles;
    }

    public void launch(Activity activity) {
        Intent intent = new Intent(activity, FsActivity.class);

        intent.putExtra(FsConstants.EXTRA_CONFIG, config);
        intent.putExtra(FsConstants.EXTRA_STORE_OPTS, storageOptions);
        intent.putExtra(FsConstants.EXTRA_AUTO_UPLOAD, autoUpload);
        intent.putExtra(FsConstants.EXTRA_SOURCES, new ArrayList<>(sources));
        intent.putExtra(FsConstants.EXTRA_ALLOW_MULTIPLE_FILES, allowMultipleFiles);

        String[] mimeTypesArray = mimeTypes.toArray(new String[0]);
        intent.putExtra(FsConstants.EXTRA_MIME_TYPES, mimeTypesArray);

        activity.startActivityForResult(intent, FILESTACK_REQUEST_CODE);
    }

    public static class Builder {
        private Config config;
        private StorageOptions storageOptions;
        private boolean autoUpload = false;
        private List<String> sources = new ArrayList<>();
        private List<String> mimeTypes = new ArrayList<>();
        private boolean allowMultipleFiles = true;

        /**
         * Sets configuration object containing all of your account info (api key, policy).
         * This param is required.
         * @param config - configuration instance
         */
        public Builder config(Config config) {
            this.config = config;
            return this;
        }

        /**
         * Storage options for uploaded files.
         */
        public Builder storageOptions(StorageOptions storageOptions) {
            this.storageOptions = storageOptions;
            return this;
        }

        /**
         * Enables automatic upload for selected files.
         * Defaults to true.
         * @param autoUpload - whether selected files should be automatically uploaded
         */
        public Builder autoUploadEnabled(boolean autoUpload) {
            this.autoUpload = autoUpload;
            return this;
        }

        /**
         * Sets sources available in a picker. Should contain values selected
         *    from {@link com.filestack.Sources}.
         * The sources will appear in the order you add them to the list.
         * Defaults to Camera, Device, Google Drive, Facebook, Instagram, and Dropbox
         * @param sources - lists of sources available in a picker
         */
        public Builder sources(List<String> sources) {
            this.sources.clear();
            this.sources.addAll(sources);
            return this;
        }

        /**
         * Restricts the types of files that can be uploaded.
         * Defaults to allowing all.
         */
        public Builder mimeTypes(List<String> mimeTypes) {
            this.mimeTypes = mimeTypes;
            return this;
        }

        /**
         * Settings this value to false restricts the selection to only one file.
         * Defaults to true.
         * @param allowMultipleFiles - whether multiple file selection should be allowed
         */
        public Builder multipleFilesSelectionEnabled(boolean allowMultipleFiles) {
            this.allowMultipleFiles = allowMultipleFiles;
            return this;
        }

        /**
         * Creates a new FilePicker instance that one can use to spawn file pickers.
         * @return a FilePicker instance
         */
        public FilestackPicker build() {
            if (config == null) {
                throw new IllegalStateException("Config cannot be null!");
            }
            return new FilestackPicker(config, storageOptions, autoUpload, sources, mimeTypes, allowMultipleFiles);
        }

    }
}
